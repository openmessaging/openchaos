package io.openmessaging.chaos.recorder;

import io.openmessaging.chaos.common.InvokeResult;

public class ResponseLogEntry {
    public int clientId;
    public String operation;
    public LogEntryType type = LogEntryType.RESPONSE;
    public InvokeResult result;
    public String value;
    public String shardingKey;
    public long timestamp;
    public long latency;

    public ResponseLogEntry(int clientId, String operation, InvokeResult result, String value, long timestamp,
        long latency) {
        this.clientId = clientId;
        this.operation = operation;
        this.result = result;
        this.value = value;
        this.timestamp = timestamp;
        this.latency = latency;
    }

    public ResponseLogEntry(int clientId, String operation, InvokeResult result, String shardingKey, String value,
        long timestamp, long latency) {
        this.clientId = clientId;
        this.operation = operation;
        this.result = result;
        this.value = value;
        this.shardingKey = shardingKey;
        this.timestamp = timestamp;
        this.latency = latency;
    }

    @Override
    public String toString() {
        return String.format("%d\t%s\t%s\t%s\t%s\t%s\t%d\t%d\n", clientId, operation, type, result, value, shardingKey, timestamp, latency);
    }
}
