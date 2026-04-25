#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  AroundU — Prod-Like Local Cluster Bootstrap
#  Provisions KIND cluster: 1 control-plane + 9 worker nodes
#
#  Usage:
#    ./k8s/setup-prod-cluster.sh          # full setup
#    ./k8s/setup-prod-cluster.sh --skip-build   # skip Docker build
#    ./k8s/setup-prod-cluster.sh --destroy      # tear down cluster
# ═══════════════════════════════════════════════════════════════
set -euo pipefail

CLUSTER_NAME="aroundu-prod"
NAMESPACE="aroundu"
IMAGE_NAME="aroundu-backend:latest"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── Colours ────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERR]${NC}   $*"; exit 1; }
section() { echo -e "\n${BOLD}${CYAN}━━━  $*  ━━━${NC}"; }

SKIP_BUILD=false
DESTROY=false
for arg in "$@"; do
  case $arg in
    --skip-build) SKIP_BUILD=true ;;
    --destroy)    DESTROY=true ;;
  esac
done

# ═══════════════════════════════════════════════════════════════
# DESTROY
# ═══════════════════════════════════════════════════════════════
if $DESTROY; then
  section "Tearing Down $CLUSTER_NAME"
  if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    kind delete cluster --name "$CLUSTER_NAME"
    success "Cluster $CLUSTER_NAME deleted"
  else
    warn "Cluster $CLUSTER_NAME not found"
  fi
  exit 0
fi

# ═══════════════════════════════════════════════════════════════
# PREFLIGHT
# ═══════════════════════════════════════════════════════════════
section "Preflight Checks"
for cmd in kind kubectl docker; do
  command -v "$cmd" &>/dev/null || error "$cmd not found — install it first"
  success "$cmd found"
done

# Check Docker is running
docker info &>/dev/null || error "Docker is not running"
success "Docker daemon running"

# ═══════════════════════════════════════════════════════════════
# BUILD IMAGE
# ═══════════════════════════════════════════════════════════════
if ! $SKIP_BUILD; then
  section "Building Spring Boot Docker Image"
  cd "$ROOT_DIR"
  info "Running Maven package (skip tests)…"
  ./mvnw -q package -DskipTests
  info "Building Docker image: $IMAGE_NAME"
  docker build -t "$IMAGE_NAME" .
  success "Image built: $IMAGE_NAME"
else
  warn "Skipping Docker build (--skip-build)"
fi

# ═══════════════════════════════════════════════════════════════
# CLUSTER SETUP
# ═══════════════════════════════════════════════════════════════
section "Creating KIND Cluster: $CLUSTER_NAME (1 CP + 9 workers)"

# Delete old cluster with same name if exists
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  warn "Existing cluster '$CLUSTER_NAME' found — deleting…"
  kind delete cluster --name "$CLUSTER_NAME"
fi

kind create cluster --config "$SCRIPT_DIR/kind-prod.yaml"
success "Cluster created"

# Set context
kubectl config use-context "kind-${CLUSTER_NAME}"

# ── Node overview ──────────────────────────────────────────────
section "Cluster Nodes"
kubectl get nodes -o wide

# ═══════════════════════════════════════════════════════════════
# INSTALL METRICS-SERVER
# ═══════════════════════════════════════════════════════════════
section "Installing metrics-server"
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Patch for KIND (disable TLS verification for kubelet)
kubectl patch deployment metrics-server \
  -n kube-system \
  --type=json \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

success "metrics-server installed"

# ═══════════════════════════════════════════════════════════════
# LOAD IMAGE INTO CLUSTER
# ═══════════════════════════════════════════════════════════════
section "Loading Docker Image into KIND cluster"
kind load docker-image "$IMAGE_NAME" --name "$CLUSTER_NAME"
success "Image loaded into all cluster nodes"

# ═══════════════════════════════════════════════════════════════
# DEPLOY STACK
# ═══════════════════════════════════════════════════════════════
section "Deploying AroundU Stack"
kubectl apply -f "$SCRIPT_DIR/prod-stack.yaml"
success "All manifests applied"

# ═══════════════════════════════════════════════════════════════
# WAIT FOR READINESS
# ═══════════════════════════════════════════════════════════════
section "Waiting for Infrastructure Pods"

info "Waiting for PostgreSQL…"
kubectl rollout status deployment/postgres -n "$NAMESPACE" --timeout=180s
success "PostgreSQL ready"

info "Waiting for Redis…"
kubectl rollout status deployment/redis -n "$NAMESPACE" --timeout=120s
success "Redis ready"

info "Waiting for AroundU app (3 replicas)…"
kubectl rollout status deployment/aroundu-app -n "$NAMESPACE" --timeout=300s
success "App ready"

info "Waiting for Prometheus…"
kubectl rollout status deployment/prometheus -n "$NAMESPACE" --timeout=120s
success "Prometheus ready"

info "Waiting for Grafana…"
kubectl rollout status deployment/grafana -n "$NAMESPACE" --timeout=120s
success "Grafana ready"

# ═══════════════════════════════════════════════════════════════
# PORT-FORWARDS  (background)
# ═══════════════════════════════════════════════════════════════
section "Setting Up Port-Forwards"

# Kill any existing port-forwards for these ports
for port in 8080 9090 3000; do
  lsof -ti :"$port" 2>/dev/null | xargs kill -9 2>/dev/null || true
done

kubectl port-forward svc/aroundu-app   8080:8080 -n "$NAMESPACE" &>/tmp/pf-app.log &
sleep 1
kubectl port-forward svc/prometheus    9090:9090 -n "$NAMESPACE" &>/tmp/pf-prometheus.log &
sleep 1
kubectl port-forward svc/grafana       3000:3000 -n "$NAMESPACE" &>/tmp/pf-grafana.log &
sleep 1

success "Port-forwards started in background"

# ═══════════════════════════════════════════════════════════════
# SMOKE TEST
# ═══════════════════════════════════════════════════════════════
section "Smoke Tests"

info "Waiting 10s for port-forwards to stabilise…"
sleep 10

# App health
if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
  success "App health: OK  → http://localhost:8080"
else
  warn "App health check failed — check: kubectl logs -n $NAMESPACE -l app=aroundu-app"
fi

# Prometheus
if curl -sf http://localhost:9090/-/ready > /dev/null 2>&1; then
  success "Prometheus: OK  → http://localhost:9090"
else
  warn "Prometheus not ready yet"
fi

# Grafana
if curl -sf http://localhost:3000/api/health > /dev/null 2>&1; then
  success "Grafana: OK     → http://localhost:3000  (admin/admin)"
else
  warn "Grafana not ready yet — may still be starting"
fi

# ═══════════════════════════════════════════════════════════════
# SUMMARY
# ═══════════════════════════════════════════════════════════════
section "Cluster Summary"
echo ""
echo -e "${BOLD}Nodes (1 CP + 9 Workers):${NC}"
kubectl get nodes -L node-type,topology.kubernetes.io/zone

echo ""
echo -e "${BOLD}Pods in namespace '$NAMESPACE':${NC}"
kubectl get pods -n "$NAMESPACE" -o wide

echo ""
echo -e "${BOLD}HPA Status:${NC}"
kubectl get hpa -n "$NAMESPACE"

echo ""
echo -e "${BOLD}${GREEN}═══════════════════════════════════════════════${NC}"
echo -e "${BOLD}  AroundU Prod-Like Cluster is LIVE!${NC}"
echo -e "${BOLD}${GREEN}═══════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${CYAN}App API:${NC}      http://localhost:8080"
echo -e "  ${CYAN}Prometheus:${NC}   http://localhost:9090"
echo -e "  ${CYAN}Grafana:${NC}      http://localhost:3000  (admin / admin)"
echo ""
echo -e "  ${YELLOW}Stress test:${NC}  ./stress-tests/run-stress-test.sh"
echo -e "  ${YELLOW}Watch pods:${NC}   kubectl get pods -n $NAMESPACE -w"
echo -e "  ${YELLOW}HPA watch:${NC}    kubectl get hpa -n $NAMESPACE -w"
echo -e "  ${YELLOW}Tear down:${NC}    ./k8s/setup-prod-cluster.sh --destroy"
echo ""
