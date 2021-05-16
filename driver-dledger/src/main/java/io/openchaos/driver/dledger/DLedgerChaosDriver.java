/**
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
package io.openchaos.driver.dledger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.PreChaosNode;
import io.openchaos.driver.dledger.config.DLedgerConfig;
import io.openchaos.driver.cache.CacheChaosClient;
import io.openchaos.driver.cache.CacheChaosDriver;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLedgerChaosDriver implements CacheChaosDriver {

    private static final Logger log = LoggerFactory.getLogger(DLedgerChaosDriver.class);
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private List<String> nodes;
    private String group = "default";
    private DLedgerConfig dLedgerConfig;


    @Override public CacheChaosClient createCacheChaosClient() {
        return new DLedgerChaosClient(group, getPeers());
    }

    @Override public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.nodes = nodes;
        this.dLedgerConfig = MAPPER.readValue(configurationFile, DLedgerConfig.class);
        if (dLedgerConfig.group != null && !dLedgerConfig.group.isEmpty()) {
            this.group = dLedgerConfig.group;
        }
    }

    @Override public void shutdown() {

    }

    @Override public ChaosNode createChaosNode(String node, List<String> nodes) {
        return new DLedgerChaosNode(node, nodes, dLedgerConfig);
    }

    @Override public PreChaosNode createPreChaosNode(String node, List<String> nodes) {
        return null;
    }

    public String getPeers() {
        if (dLedgerConfig.peers != null && !dLedgerConfig.peers.isEmpty()) {
            return dLedgerConfig.peers;
        } else {
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < nodes.size(); i++) {
                res.append("n" + i + "-" + nodes.get(i) + ":20911;");
            }
            return res.toString();
        }
    }
}
