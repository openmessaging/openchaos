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
package io.openchaos.driver.dledger;

import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.dledger.config.DLedgerConfig;
import io.openchaos.driver.kv.KVNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DLedgerChaosNode implements KVNode {

    private static final Logger log = LoggerFactory.getLogger(DLedgerChaosNode.class);
    private String node;
    private List<String> nodes;
    private DLedgerConfig dLedgerConfig;
    private String installDir = "dledger-chaos-test";
    private String dledgerVersion = "0.2.0";
    private String storeBaseDir = "/tmp/dledgerstore";
    private String group = "default";
    private static final String DLEDGER_PROCESS_NAME = "DLedger.jar";

    public DLedgerChaosNode(String node, List<String> nodes, DLedgerConfig dLedgerConfig) {
        this.node = node;
        this.nodes = nodes;
        this.dLedgerConfig = dLedgerConfig;

        if (dLedgerConfig.installDir != null && !dLedgerConfig.installDir.isEmpty()) {
            this.installDir = dLedgerConfig.installDir;
        }
        if (dLedgerConfig.dledgerVersion != null && !dLedgerConfig.dledgerVersion.isEmpty()) {
            this.dledgerVersion = dLedgerConfig.dledgerVersion;
        }
        if (dLedgerConfig.storeBaseDir != null && !dLedgerConfig.storeBaseDir.isEmpty()) {
            this.storeBaseDir = dLedgerConfig.storeBaseDir;
        }
        if (dLedgerConfig.group != null && !dLedgerConfig.group.isEmpty()) {
            this.group = dLedgerConfig.group;
        }
    }

    @Override public void setup() {
        try {
            //Download dledger package
            log.info("Node {} download dledger...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                String.format("wget https://github.com/openmessaging/openmessaging-storage-dledger/archive/dledger-%s.zip -O dledger.zip", dledgerVersion),
                "unzip -o dledger.zip", "rm -f dledger.zip", "mv openmessaging-storage-dledger*/* .", "rm -rf openmessaging-storage-dledger*");
            SshUtil.execCommandInDir(node, installDir, "mvn clean install -DskipTests");
        } catch (Exception e) {
            log.error("Node {} setup dledger node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void teardown() {
        stop();
        try {
            SshUtil.execCommand(node, String.format("rm -rf %s", storeBaseDir));
        } catch (Exception e) {
            log.error("Node {} teardown dledger failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void start() {
        try {
            //Start dledger
            log.info("Node {} start dledger...", node);
            String cmdline = String.format("nohup java -jar target/DLedger.jar server -g %s -i %s -p '%s' -s %s > dledger.log 2>&1 &",
                group, "n" + nodes.indexOf(node), getPeers(), storeBaseDir);
            log.info("Node {} execute cmd: " + cmdline);
            SshUtil.execCommandInDir(node, installDir, cmdline);
        } catch (Exception e) {
            log.error("Node {} start dledger process failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void stop() {
        try {
            KillProcessUtil.kill(node, DLEDGER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop dledger processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void kill() {
        try {
            KillProcessUtil.forceKill(node, DLEDGER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop dledger processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void pause() {
        try {
            PauseProcessUtil.suspend(node, DLEDGER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop dledger processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void resume() {
        try {
            PauseProcessUtil.resume(node, DLEDGER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop dledger processes failed", node, e);
            throw new RuntimeException(e);
        }
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
