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
package io.openchaos.driver.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openchaos.common.InvokeResult;
import io.openchaos.common.NacosMessage;
import io.openchaos.driver.nacos.config.NacosConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Executor;

public class NacosChaosClient implements NacosClient {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger LOG = LoggerFactory.getLogger(NacosClient.class);

    public ConfigService nacosConfigPub;
    public ConfigService nacosConfigListener;
    public NacosConfig nacosConfig;
    public NacosCallback nacosCallback;
    public long sendtimestamp;
    private int id;
    private int num;
    private int threshold;
    private String dataId;
    private String group;
    private Map<String, Long> configMap = new HashMap<>();


    public NacosChaosClient(NacosConfig nacosConfig ,int id) {
        this.nacosConfig = nacosConfig;
        this.id = id;
        prepare();
    }


    public NacosChaosClient(NacosConfig nacosConfig ,int id,NacosCallback nacosCallback) {
        this.nacosConfig = nacosConfig;
        this.id = id;
        this.nacosCallback = nacosCallback;
        this.threshold = nacosConfig.threshold;
        Nacosprepare();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void prepare() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.serverLIST);

        properties.put("keyId", "alias/acs/mse");
        properties.put("regionId", "cn-hangzhou");
        try {
            nacosConfigPub = NacosFactory.createConfigService(properties);
            nacosConfigListener = NacosFactory.createConfigService(properties);
            String name;
            for (int i = 0; i < nacosConfig.num; i++) {
                name = "client: " + id + "dataId: " + nacosConfig.dataIds.get(id) + i + "group: " + nacosConfig.group.get(0);
//                boolean isSuccess = nacosConfigPub.removeConfig(nacosConfig.dataIds.get(id) + i, nacosConfig.group.get(0));
                nacosConfigListener.addListener(nacosConfig.dataIds.get(id) + i, nacosConfig.group.get(0), new DefaultConfigListener(name));
            }
        } catch (NacosException e) {
            LOG.info("Nacos configservice create or listen error");
        }
    }
    public void Nacosprepare() {
        //add record listener
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.serverLIST);
        properties.put("keyId", "alias/acs/mse");
        properties.put("regionId", "cn-hangzhou");
        try {
            nacosConfigPub = NacosFactory.createConfigService(properties);
            nacosConfigListener = NacosFactory.createConfigService(properties);
            for (int i = 0; i < nacosConfig.num; i++) {
                String tempdataId = nacosConfig.dataIds.get(id) + i;
                String tempgroup = nacosConfig.group.get(0);


                nacosConfigListener.addListener(tempdataId, tempgroup, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String s) {
                        NacosMessage message = new NacosMessage(id,tempdataId,tempgroup,s,System.currentTimeMillis());
                        if (configMap.containsKey(s)) {
                            message.setPubTimestamp(configMap.get(s),InvokeResult.SUCCESS);
                            if (message.subTimestamp - message.pubTimestamp > threshold) {
                                message.setResult(InvokeResult.TimeOut);
                            }
                            configMap.remove(s);
                        } else {
                            message.setPubTimestamp(-1,InvokeResult.FAILURE);
                        }
                        nacosCallback.messageReceived(message);
                        LOG.info(id + " receive content: " + s);

                    }
                });
            }
        } catch (NacosException e) {
            LOG.info("Nacos configservice create or listen error");
        }
    }

    @Override
    public InvokeResult put(Optional<String> key, String dataId,String group,String config) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean result = false;

        try {
            result = nacosConfigPub.publishConfig(dataId, group, config);
            sendtimestamp = System.currentTimeMillis();
            if (result) {
                configMap.put(config,sendtimestamp);
                LOG.info("nacos config publish SUCCESS");
                return InvokeResult.SUCCESS;
            } else {
                LOG.warn("nacos config publish error");
                return InvokeResult.FAILURE;
            }
        } catch (NacosException e) {
            LOG.info("CONFIG PUBLISH ERROR");
        }
        return InvokeResult.SUCCESS;
    }

    public long getSendtimestamp() {
        return sendtimestamp;
    }

    @Override
    public List<String> getAll(Optional<String> key, int putInvokeCount) {
        return null;
    }

    @Override
    public List<String> getAll(Optional<String> key) {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }
}
