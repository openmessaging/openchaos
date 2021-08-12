package io.openchaos.common;

public class NacosMessage {
    public int clientid;
    public String dataId;
    public String group;
    public String config;
    public long pubTimestamp;
    public long subTimestamp;
    public InvokeResult result;

    public NacosMessage(int clientid, String dataId, String group, String config, long subTimestamp){
        this.clientid = clientid;
        this.dataId = dataId;
        this.group = group;
        this.config = config;
        this.subTimestamp = subTimestamp;
    }
    public void setPubTimestamp(long pubTimestamp, InvokeResult result){
        this.pubTimestamp = pubTimestamp;
        this.result = result;
    }
    public void setResult(InvokeResult result){
        this.result = result;
    }
}
