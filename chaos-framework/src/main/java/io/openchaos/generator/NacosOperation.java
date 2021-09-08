/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.openchaos.generator;



public class NacosOperation {
    private String invokeOperation;
    private String dataID;
    private String group;
    private String pubConfig;


    public NacosOperation(String invokeOperation,String dataID,String group,int num,String configContent) {
        this.invokeOperation = invokeOperation;
        this.dataID = dataID + num;
        this.group = group;
        this.pubConfig = configContent;
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

    public void setDataID(String dataID) {
        this.dataID = dataID;
    }
    public  String getDataID() {
        return dataID;
    }
    public void setGroup(String group) {
        this.group = group;
    }
    public  String getGroup() {
        return group;
    }
    public String getValue() {
        //get MD5 value of the config
        return "dataID:\t" + dataID + "\tgroup:\t" + group + "\t" + "pubConfig\t" + MD5.MD5_16(pubConfig) ;
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
