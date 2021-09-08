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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.nacos.config.NacosConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NacosChaosDriver implements NacosDriver {

    private static final Logger log = LoggerFactory.getLogger(NacosChaosDriver.class);
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private List<String> nodes;

    private NacosConfig nacosConfig;


    @Override public NacosClient createClient(int id) {
        return new NacosChaosClient(nacosConfig,id);
    }
    @Override public NacosClient createClient(int id,NacosCallback nacosCallback) {
        return new NacosChaosClient(nacosConfig,id,nacosCallback);
    }

    @Override
    public String getMetaNode() {
        return null;
    }

    @Override
    public String getMetaName() {
        return null;
    }

    @Override public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.nodes = nodes;
        this.nacosConfig = MAPPER.readValue(configurationFile, NacosConfig.class);
    }

    @Override public void shutdown() {

    }

    @Override public ChaosNode createChaosNode(String node, List<String> nodes) {
        return null;
    }

    @Override public MetaNode createChaosMetaNode(String node, List<String> nodes) {
        return null;
    }

    @Override
    public String getStateName() {
        return null;
    }



    public List<String> getgroup() {
        return nacosConfig.group;
    }
    public List<String> getdataIds() {
        return nacosConfig.dataIds;
    }
    public int getNUM() { return nacosConfig.num; }
}


