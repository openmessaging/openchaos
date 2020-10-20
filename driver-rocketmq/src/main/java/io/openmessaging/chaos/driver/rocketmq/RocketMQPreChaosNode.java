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

package io.openmessaging.chaos.driver.rocketmq;

import io.openmessaging.chaos.common.utils.KillProcessUtil;
import io.openmessaging.chaos.common.utils.PauseProcessUtil;
import io.openmessaging.chaos.common.utils.SshUtil;
import io.openmessaging.chaos.driver.PreChaosNode;
import io.openmessaging.chaos.driver.rocketmq.config.RocketMQConfig;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nodes for nameserver
 */
public class RocketMQPreChaosNode implements PreChaosNode {

    private static final String NAMESERVER_PROCESS_NAME = "NamesrvStartup";
    private static final Logger log = LoggerFactory.getLogger(RocketMQPreChaosNode.class);
    private String installDir = "rocketmq-chaos-test";
    private String rocketmqVersion = "4.6.0";
    private String node;
    private List<String> nodes;

    public RocketMQPreChaosNode(String node, List<String> nodes, RocketMQConfig rmqConfig) {
        this.node = node;
        this.nodes = nodes;
        if (rmqConfig.installDir != null && !rmqConfig.installDir.isEmpty()) {
            this.installDir = rmqConfig.installDir;
        }
        if (rmqConfig.rocketmqVersion != null && !rmqConfig.rocketmqVersion.isEmpty()) {
            this.rocketmqVersion = rmqConfig.rocketmqVersion;
        }
    }

    @Override public void setup() {
        try {
            //Download rocketmq package
            log.info("Node {} download rocketmq for nameserver ...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                String.format("curl https://archive.apache.org/dist/rocketmq/%s/rocketmq-all-%s-bin-release.zip -o rocketmq.zip", rocketmqVersion, rocketmqVersion),
                "unzip rocketmq.zip", "rm -f rocketmq.zip", "mv rocketmq-all*/* .", "rmdir rocketmq-all*");
            log.info("Node {} download rocketmq for nameserver success", node);

            //For docker test, because the memory of local computer is too small
            SshUtil.execCommandInDir(node, installDir, "sed -i  's/-Xms4g -Xmx4g -Xmn2g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m/-Xms500m -Xmx500m -Xmn250m -XX:MetaspaceSize=16m -XX:MaxMetaspaceSize=40m/g' bin/runserver.sh");
        } catch (Exception e) {
            log.error("Node {} setup nameserver node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void teardown() {
        stop();
    }

    @Override public void start() {
        try {
            //Start nameserver
            log.info("Node {} start nameserver...", node);
            SshUtil.execCommandInDir(node, installDir, "nohup sh bin/mqnamesrv > nameserver.log 2>&1 &");
        } catch (Exception e) {
            log.error("Node {} start nameserver node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void stop() {
        try {
            KillProcessUtil.kill(node, NAMESERVER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop nameserver processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void kill() {
        try {
            KillProcessUtil.forceKill(node, NAMESERVER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} kill nameserver processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void pause() {
        try {
            PauseProcessUtil.suspend(node, NAMESERVER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} suspend nameserver processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void resume() {
        try {
            PauseProcessUtil.resume(node, NAMESERVER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} resume nameserver processes failed", node, e);
            throw new RuntimeException(e);
        }
    }
}
