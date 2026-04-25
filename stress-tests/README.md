# AroundU Java Backend - Stress Testing

This directory contains stress testing infrastructure for load testing the AroundU Java backend using **Vegeta** - the same tool used by the Go backend.

## Prerequisites

### Install Vegeta

**macOS:**

```bash
brew install vegeta
```

**Linux:**

```bash
go install github.com/tsenart/vegeta@latest
```

**Verify installation:**

```bash
vegeta --version
```

## Quick Start

### 1. Local Testing (Docker Compose)

Ensure the backend is running:

```bash
cd ..
docker compose up -d
```

Run stress tests:

```bash
./run-tests.sh --url http://localhost:8080 --rps "1000,5000,10000,20000"
```

### 2. In-Cluster Testing (Kubernetes)

For accurate results without local networking overhead:

```bash
./run-tests.sh --mode k8s
```

This deploys a Vegeta pod inside the cluster and runs tests against the internal service.

## Test Scenarios

| RPS Level | Purpose     | Expected Behavior              |
| --------- | ----------- | ------------------------------ |
| 1000      | Baseline    | Single pod handles comfortably |
| 5000      | Medium load | May trigger HPA scale-up       |
| 10000     | High load   | HPA should scale to 2-3 pods   |
| 20000     | Peak load   | Full cluster utilization       |

## Configuration Options

```bash
./run-tests.sh [OPTIONS]

Options:
  --url URL        Base URL (default: http://localhost:8080)
  --duration DUR   Test duration per level (default: 30s)
  --rps LEVELS     Comma-separated RPS levels (default: 1000,5000,10000,20000)
  --mode MODE      Test mode: local or k8s (default: local)
  --token TOKEN    Auth token for authenticated endpoints
```

## Monitoring During Tests

### Watch HPA Scaling

```bash
kubectl get hpa -n aroundu -w
```

### Watch Pod Count

```bash
kubectl get pods -n aroundu -w
```

### Watch Resource Usage

```bash
kubectl top pods -n aroundu
```

### Grafana Dashboard

Access `http://<server-ip>:3000` for real-time metrics visualization.

## Results

Results are saved in `results/` directory:

- `*.bin` - Raw Vegeta binary results (can be replayed)
- `*.json` - JSON formatted metrics
- `stress_test_report_*.md` - Human-readable summary

### Example JSON Output

```json
{
  "latencies": {
    "total": 1234567890,
    "mean": 12345678,
    "50th": 10000000,
    "90th": 25000000,
    "95th": 50000000,
    "99th": 100000000,
    "max": 500000000
  },
  "requests": 30000,
  "rate": 1000.123,
  "throughput": 998.456,
  "success": 0.9998
}
```

## HPA Configuration

The app is configured with auto-scaling:

```yaml
minReplicas: 1
maxReplicas: 10 # Updated for stress testing
metrics:
  - cpu: 60% utilization
  - memory: 80% utilization
```

## Troubleshooting

### Test fails with "connection refused"

- Ensure the backend is running and healthy
- Check: `curl http://localhost:8080/actuator/health`

### Low throughput despite high RPS

- Check CPU/memory limits in deployment
- Increase HPA maxReplicas
- Check database connection pool size

### High latency at high RPS

- Enable connection pooling
- Increase JVM heap size
- Check for GC pauses in logs

## Seeding Test Data

To generate dummy data for realistic testing:

```bash
# Via API (requires admin auth)
curl -X POST http://localhost:8080/api/v1/admin/seed-stress-data \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"jobs": 1000, "users": 500}'
```
