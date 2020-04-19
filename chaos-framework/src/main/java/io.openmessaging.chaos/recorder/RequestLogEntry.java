package io.openmessaging.chaos.recorder;

public class RequestLogEntry {
    public int clientId;
    public String operation;
    public LogEntryType type = LogEntryType.REQUEST;
    //only for enqueue
    public String value;
    //only for enqueue
    public String shardingKey;
    public long timestamp;

    //for enqueue
    public RequestLogEntry(int clientId, String operation, String value, long timestamp) {
        this.clientId = clientId;
        this.operation = operation;
        this.value = value;
        this.timestamp = timestamp;
    }

    public RequestLogEntry(int clientId, String operation, String shardingKey, String value,
        long timestamp) {
        this.clientId = clientId;
        this.operation = operation;
        this.value = value;
        this.shardingKey = shardingKey;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("%d\t%s\t%s\t%s\t%s\t%d\n", clientId, operation, type, value, shardingKey, timestamp);
    }
}
