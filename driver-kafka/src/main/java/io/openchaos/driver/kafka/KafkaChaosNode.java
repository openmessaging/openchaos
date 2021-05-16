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

package io.openchaos.driver.kafka;

import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.kafka.config.KafkaBrokerConfig;
import io.openchaos.driver.kafka.config.KafkaConfig;
import io.openchaos.driver.mq.MQChaosNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

public class KafkaChaosNode implements MQChaosNode {

    private static final String BROKER_PROCESS_NAME = "Kafka";
    private static final Logger log = LoggerFactory.getLogger(KafkaChaosNode.class);
    private String node;
    private List<String> nodes;
    private KafkaConfig kafkaConfig;
    private KafkaBrokerConfig kafkaBrokerConfig;
    private String installDir = "kafka-chaos-test";
    private String configureFilePath = "config/server.properties";
    private String scalaVersion = "2.11";
    private String kafkaVersion = "1.1.0";

    public KafkaChaosNode(String node, List<String> nodes, KafkaConfig kafkaConfig, KafkaBrokerConfig kafkaBrokerConfig) {
        this.node = node;
        this.nodes = nodes;
        this.kafkaConfig = kafkaConfig;
        this.kafkaBrokerConfig = kafkaBrokerConfig;
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

            Field[] fields = kafkaBrokerConfig.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                String value = (String) fields[i].get(kafkaBrokerConfig);
                if (value != null && !value.isEmpty()) {
                    SshUtil.execCommandInDir(node, installDir, String.format("echo '\n%s' >> %s", formatKey(name) + "=" + value, configureFilePath));
                }
            }
            SshUtil.execCommandInDir(node, installDir, String.format("echo '\n%s' >> %s", "broker.id=" + nodes.indexOf(node), configureFilePath));
            SshUtil.execCommandInDir(node, installDir, String.format("echo '\n%s' >> %s", "host.name=" + node, configureFilePath));
        } catch (Exception e) {
            log.error("Node {} setup kafka node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        try {
            //Start broker
            log.info("Node {} start broker...", node);
            SshUtil.execCommandInDir(node, installDir, String.format("nohup sh bin/kafka-server-start.sh '%s' > broker.log 2>&1 &"
                , configureFilePath));
        } catch (Exception e) {
            log.error("Node {} start kafka node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void teardown() {
        try {
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
        } catch (Exception e) {
            log.error("Node {} teardown kafka node failed", node, e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public void stop() {
        try {
            KillProcessUtil.kill(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop kafka processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void kill() {
        try {
            KillProcessUtil.forceKill(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} kill kafka processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        try {
            PauseProcessUtil.suspend(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} pause kafka processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        try {
            PauseProcessUtil.resume(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} resume kafka processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    private String formatKey(String name) {
        return name.replaceAll("[A-Z]", ".$0").toLowerCase();
    }

}
