package io.openchaos.checker.result;

public class NacosEndToEndLatencyResult extends TestResult{
    public String e2eTotalLatency;
    public String e2eAveLatency;
    public String e2e50Latency;
    public String e2e75Latency;
    public String e2e85Latency;
    public String e2e90Latency;
    public String e2e95Latency;
    public String minLatency;
    public String maxLatency;
    public String timeOutCount;
    public NacosEndToEndLatencyResult() {
        super("NacosEndToEndLatencyResult");
    }

}
