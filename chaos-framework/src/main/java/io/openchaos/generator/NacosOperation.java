package io.openchaos.generator;

import com.sun.corba.se.spi.orb.Operation;

public class NacosOperation {
    private String invokeOperation;
    private String dataID;
    private String group;
    private String pubConfig;


    public NacosOperation(String invokeOperation,String dataID,String group,int num,String config_content) {
        this.invokeOperation = invokeOperation;
        this.dataID = dataID+num;
        this.group = group;
        this.pubConfig = config_content;
    }

    public String getInvokeOperation() {
        return invokeOperation;
    }

    public void setInvokeOperation(String invokeOperation) {
        this.invokeOperation = invokeOperation;
    }

    public String getPubConfig() {
        return pubConfig;
    }


    public void setPubConfig(String pubConfig) {
        this.pubConfig = pubConfig;
    }

    public void setDataID(String dataID){
        this.dataID = dataID;
    }
    public  String getDataID(){
        return dataID;
    }
    public void setGroup(String group){
        this.group = group;
    }
    public  String getGroup(){
        return group;
    }
    public String getValue() {
        //get MD5 value of the config
        return "dataID:\t"+dataID + "\tgroup:\t" + group +"\t"+"pubConfig\t" + MD5.MD5_16(pubConfig) ;
    }

    @Override
    public String toString() {
        return "NacosOperation{" +
                "invokeOperation='" + invokeOperation + '\'' +
                ", dataID='" + dataID + '\'' +
                ", group='" + group + '\'' +
                ", pubConfig=" + pubConfig +
                '}';
    }
}
