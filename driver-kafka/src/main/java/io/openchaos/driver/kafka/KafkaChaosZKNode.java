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

package io.openchaos.driver.kafka;

import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.kafka.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Nodes for zookeeper
 */
public class KafkaChaosZKNode implements MetaNode {

    private static final String ZOOKEEPER_PROCESS_NAME = "QuorumPeerMain";
    private static final Logger log = LoggerFactory.getLogger(KafkaChaosZKNode.class);
    private String node;
    private List<String> nodes;
    private String installDir = "kafka-chaos-test";
    private String configureFilePath = "config/zookeeper.properties";
    private String scalaVersion = "2.11";
    private String kafkaVersion = "1.1.0";

    public KafkaChaosZKNode(String node, List<String> nodes, KafkaConfig kafkaConfig) {
        this.node = node;
        this.nodes = nodes;
        if (kafkaConfig.installDir != null && !kafkaConfig.installDir.isEmpty()) {
            this.installDir = kafkaConfig.installDir;
        }
        if (kafkaConfig.configureFilePath != null && !kafkaConfig.configureFilePath.isEmpty()) {
            this.configureFilePath = kafkaConfig.configureFilePath;
        }
        if (kafkaConfig.scalaVersion != null && !kafkaConfig.scalaVersion.isEmpty()) {
            this.scalaVersion = kafkaConfig.scalaVersion;
        }
        if (kafkaConfig.kafkaVersion != null && !kafkaConfig.kafkaVersion.isEmpty()) {
            this.kafkaVersion = kafkaConfig.kafkaVersion;
        }
    }

    @Override
    public void setup() {
        try {
            //Download kafka package
            log.info("Node {} download kafka...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                    String.format("curl http://archive.apache.org/dist/kafka/%s/kafka_%s-%s.tgz -o kafka.tgz", kafkaVersion, scalaVersion, kafkaVersion),
                    "tar -zxf kafka.tgz", "rm -f kafka.tgz", "mv kafka*/* .", "rmdir kafka*");
            log.info("Node {} download kafka success", node);

        } catch (Exception e) {
            log.error("Node {} setup kafka node failed", node, e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public void teardown() {
        stop();
    }

    @Override
    public void start() {
        try {
            //Start zookeeper
            log.info("Node {} start zookeeper...", node);
//            SshUtil.execCommandInDir(node, installDir, String.format("nohup sh bin/zookeeper-server-start.sh '%s' > zookeeper.log 2>&1 &"
//                , configureFilePath));
            SshUtil.execCommandInDir(node, installDir, String.format("bin/zookeeper-server-start.sh -daemon config/zookeeper.properties > zookeeper.log 2>&1 &"));
        } catch (Exception e) {
            log.error("Node {} start zookeeper node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            KillProcessUtil.kill(node, ZOOKEEPER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop zookeeper processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void kill() {
        try {
            KillProcessUtil.forceKill(node, ZOOKEEPER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} kill zookeeper processes failed", node, e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void pause() {
        try {
            PauseProcessUtil.suspend(node, ZOOKEEPER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} pause zookeeper processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        try {
            PauseProcessUtil.resume(node, ZOOKEEPER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} resume zookeeper processes failed", node, e);
            throw new RuntimeException(e);
        }
    }
}
