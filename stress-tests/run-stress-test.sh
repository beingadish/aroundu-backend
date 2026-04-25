#!/bin/bash
#
# AroundU Stress Test Runner with Real-Time Monitoring
# Usage: ./run-stress-test.sh [RPS] [DURATION] [ENDPOINT]
#
# Examples:
#   ./run-stress-test.sh              # Default: 100 RPS, 30s, health endpoint
#   ./run-stress-test.sh 500 60       # 500 RPS for 60 seconds
#   ./run-stress-test.sh 1000 120 /api/jobs  # Custom endpoint
#

set -e

# Configuration
RPS=${1:-100}
DURATION=${2:-30}
ENDPOINT=${3:-/actuator/health}
BASE_URL=${BASE_URL:-http://localhost:8081}
NAMESPACE=${NAMESPACE:-aroundu}
RESULTS_DIR="$(dirname "$0")/results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_FILE="${RESULTS_DIR}/stress_${RPS}rps_${TIMESTAMP}.txt"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Banner
print_banner() {
    echo -e "${CYAN}"
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║     🚀 AroundU Stress Test Runner with Real-Time Monitoring    ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Check dependencies
check_deps() {
    local missing=()
    for cmd in vegeta kubectl curl jq; do
        if ! command -v $cmd &> /dev/null; then
            missing+=($cmd)
        fi
    done
    
    if [ ${#missing[@]} -ne 0 ]; then
        echo -e "${RED}Missing dependencies: ${missing[*]}${NC}"
        echo "Install with: brew install ${missing[*]}"
        exit 1
    fi
}

# Check if target is reachable
check_target() {
    echo -e "${YELLOW}▶ Checking target: ${BASE_URL}${ENDPOINT}${NC}"
    if curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${ENDPOINT}" | grep -q "200\|401\|403"; then
        echo -e "${GREEN}✓ Target is reachable${NC}"
    else
        echo -e "${RED}✗ Target unreachable. Is port-forward running?${NC}"
        echo -e "${YELLOW}  Try: kubectl port-forward -n ${NAMESPACE} svc/aroundu-app 8081:8080 &${NC}"
        exit 1
    fi
}

# Display current cluster state
show_cluster_state() {
    echo -e "\n${BLUE}${BOLD}═══ Current Cluster State ═══${NC}"
    echo -e "${CYAN}HPA Status:${NC}"
    kubectl get hpa -n ${NAMESPACE} 2>/dev/null || echo "  No HPA found"
    echo -e "\n${CYAN}Pod Status:${NC}"
    kubectl get pods -n ${NAMESPACE} -l app=aroundu-app --no-headers 2>/dev/null | head -5
    echo -e "\n${CYAN}Pod Metrics:${NC}"
    kubectl top pods -n ${NAMESPACE} -l app=aroundu-app 2>/dev/null || echo "  Metrics not available yet"
}

# Start monitoring in background
start_monitoring() {
    local monitor_file="${RESULTS_DIR}/monitor_${TIMESTAMP}.log"
    
    echo -e "\n${YELLOW}▶ Starting real-time monitoring...${NC}"
    echo -e "${CYAN}  Monitor log: ${monitor_file}${NC}"
    
    # Start watch in a new terminal window (macOS)
    osascript -e "
        tell application \"Terminal\"
            do script \"watch -n 2 'echo \\\"═══ HPA Status ═══\\\"; kubectl get hpa -n ${NAMESPACE} 2>/dev/null; echo \\\"\\\"; echo \\\"═══ Pods ($(date)) ═══\\\"; kubectl get pods -n ${NAMESPACE} -l app=aroundu-app 2>/dev/null; echo \\\"\\\"; echo \\\"═══ Pod Metrics ═══\\\"; kubectl top pods -n ${NAMESPACE} -l app=aroundu-app 2>/dev/null; echo \\\"\\\"; echo \\\"═══ HPA Events ═══\\\"; kubectl get events -n ${NAMESPACE} --sort-by=.lastTimestamp 2>/dev/null | grep -i hpa | tail -5'\"
            set custom title of front window to \"AroundU Stress Test Monitor\"
        end tell
    " 2>/dev/null || {
        # Fallback: run in background and log
        echo -e "${YELLOW}  (Monitoring in background - check ${monitor_file})${NC}"
        while true; do
            {
                echo "═══ $(date) ═══"
                kubectl get hpa -n ${NAMESPACE} 2>/dev/null
                kubectl get pods -n ${NAMESPACE} -l app=aroundu-app 2>/dev/null
                kubectl top pods -n ${NAMESPACE} 2>/dev/null
                echo ""
            } >> "$monitor_file"
            sleep 5
        done &
        MONITOR_PID=$!
    }
}

# Run stress test
run_stress_test() {
    local target="${BASE_URL}${ENDPOINT}"
    
    echo -e "\n${BLUE}${BOLD}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}${BOLD}  🔥 Starting Stress Test${NC}"
    echo -e "${BLUE}${BOLD}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "  ${CYAN}Target:${NC}    ${target}"
    echo -e "  ${CYAN}Rate:${NC}      ${RPS} requests/second"
    echo -e "  ${CYAN}Duration:${NC}  ${DURATION} seconds"
    echo -e "  ${CYAN}Total:${NC}     $((RPS * DURATION)) requests"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"
    
    # Show live progress
    echo -e "${YELLOW}▶ Running test... (${DURATION}s)${NC}\n"
    
    # Run vegeta and capture results
    echo "GET ${target}" | vegeta attack -rate=${RPS} -duration=${DURATION}s 2>&1 | tee /tmp/vegeta_result_$$.bin | vegeta encode | vegeta report -type=text | tee "${RESULT_FILE}"
    
    # Generate histogram
    echo -e "\n${CYAN}Latency Histogram:${NC}"
    cat /tmp/vegeta_result_$$.bin 2>/dev/null | vegeta report -type=hist[0,5ms,10ms,25ms,50ms,100ms,250ms,500ms,1s] 2>/dev/null || true
    
    # Cleanup
    rm -f /tmp/vegeta_result_$$.bin
}

# Show final stats
show_final_stats() {
    echo -e "\n${BLUE}${BOLD}═══ Final Cluster State ═══${NC}"
    
    echo -e "\n${CYAN}HPA Status:${NC}"
    kubectl get hpa -n ${NAMESPACE} 2>/dev/null
    
    echo -e "\n${CYAN}Pod Count:${NC}"
    local pod_count=$(kubectl get pods -n ${NAMESPACE} -l app=aroundu-app --no-headers 2>/dev/null | wc -l)
    echo -e "  ${GREEN}${pod_count} pods running${NC}"
    
    echo -e "\n${CYAN}Pod Resources:${NC}"
    kubectl top pods -n ${NAMESPACE} -l app=aroundu-app 2>/dev/null || echo "  Metrics unavailable"
    
    echo -e "\n${CYAN}Results saved to:${NC} ${RESULT_FILE}"
}

# Progressive load test
run_progressive_test() {
    echo -e "\n${BLUE}${BOLD}═══ Progressive Load Test ═══${NC}"
    echo -e "${CYAN}Testing at: 100 → 250 → 500 → 750 → 1000 RPS${NC}\n"
    
    for rate in 100 250 500 750 1000; do
        echo -e "\n${YELLOW}━━━ Testing ${rate} RPS for 20s ━━━${NC}"
        
        local result=$(echo "GET ${BASE_URL}${ENDPOINT}" | vegeta attack -rate=${rate} -duration=20s | vegeta report -type=json)
        
        local success_ratio=$(echo "$result" | jq -r '.success')
        local mean_latency=$(echo "$result" | jq -r '.latencies.mean / 1000000 | floor')
        local p99_latency=$(echo "$result" | jq -r '.latencies."99th" / 1000000 | floor')
        
        # Color based on success rate
        if (( $(echo "$success_ratio >= 0.99" | bc -l) )); then
            status_color=$GREEN
            status="✓"
        elif (( $(echo "$success_ratio >= 0.95" | bc -l) )); then
            status_color=$YELLOW
            status="⚠"
        else
            status_color=$RED
            status="✗"
        fi
        
        printf "${status_color}${status} ${rate} RPS: %.1f%% success, mean: ${mean_latency}ms, p99: ${p99_latency}ms${NC}\n" $(echo "$success_ratio * 100" | bc)
        
        # Check HPA scaling
        local replicas=$(kubectl get hpa -n ${NAMESPACE} -o jsonpath='{.items[0].status.currentReplicas}' 2>/dev/null || echo "?")
        echo -e "  ${CYAN}Current replicas: ${replicas}${NC}"
        
        # Brief pause between tests
        sleep 5
    done
}

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Cleaning up...${NC}"
    [ -n "$MONITOR_PID" ] && kill $MONITOR_PID 2>/dev/null
    exit 0
}

trap cleanup SIGINT SIGTERM

# Main menu
main_menu() {
    echo -e "\n${CYAN}Select test mode:${NC}"
    echo "  1) Quick test (${RPS} RPS for ${DURATION}s)"
    echo "  2) Progressive load test (100 → 1000 RPS)"
    echo "  3) Sustained high load (500 RPS for 120s)"
    echo "  4) Custom test"
    echo "  5) Monitor only (no load)"
    echo ""
    read -p "Choice [1-5]: " choice
    
    case $choice in
        1) run_stress_test ;;
        2) run_progressive_test ;;
        3) RPS=500; DURATION=120; run_stress_test ;;
        4) 
            read -p "RPS: " RPS
            read -p "Duration (seconds): " DURATION
            read -p "Endpoint [${ENDPOINT}]: " custom_endpoint
            ENDPOINT=${custom_endpoint:-$ENDPOINT}
            run_stress_test
            ;;
        5) 
            echo -e "${YELLOW}Monitoring mode - Press Ctrl+C to exit${NC}"
            watch -n 2 "kubectl get hpa -n ${NAMESPACE}; echo ''; kubectl get pods -n ${NAMESPACE} -l app=aroundu-app; echo ''; kubectl top pods -n ${NAMESPACE} -l app=aroundu-app 2>/dev/null"
            ;;
        *) run_stress_test ;;
    esac
}

# Main execution
main() {
    print_banner
    check_deps
    
    # Create results directory
    mkdir -p "${RESULTS_DIR}"
    
    check_target
    show_cluster_state
    
    # If arguments provided, run directly; otherwise show menu
    if [ "$#" -gt 0 ] && [ "$1" != "--menu" ]; then
        start_monitoring
        run_stress_test
    else
        main_menu
    fi
    
    show_final_stats
    
    echo -e "\n${GREEN}${BOLD}✓ Stress test complete!${NC}"
    echo -e "${CYAN}  View Grafana dashboard for detailed metrics${NC}"
}

main "$@"
