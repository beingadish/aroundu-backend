# Stress Test Results

Results from stress test runs will be saved here.

Each run generates:

- `<test>_<rps>rps_<timestamp>.bin` - Raw Vegeta binary data
- `<test>_<rps>rps_<timestamp>.json` - JSON metrics
- `stress_test_report_<timestamp>.md` - Summary report

## Analyzing Results

Replay a binary result:

```bash
vegeta report < health_1000rps_2026-04-20.bin
```

Generate histogram:

```bash
vegeta report -type=hist[0,5ms,10ms,50ms,100ms,500ms] < health_1000rps_2026-04-20.bin
```

Plot latency over time (requires gnuplot):

```bash
vegeta plot < health_1000rps_2026-04-20.bin > plot.html
```
