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

import io.openmessaging.chaos.common.utils.PauseProcessUtil;
import io.openmessaging.chaos.common.utils.SshUtil;
import io.openmessaging.chaos.driver.mq.MQChaosNode;
import io.openmessaging.chaos.driver.rocketmq.config.RocketMQBrokerConfig;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQChaosNode implements MQChaosNode {

    private static final String INSTALL_DIR = "rocketmq-chaos-test";
    private static final String ROCKETMQ_VERSION = "4.6.0";
    private static final Logger log = LoggerFactory.getLogger(RocketMQChaosNode.class);
    private String node;
    private List<String> nodes;
    private RocketMQBrokerConfig rmqBrokerConfig;

    public RocketMQChaosNode(String node, List<String> nodes, RocketMQBrokerConfig rmqBrokerConfig) {
        this.node = node;
        this.nodes = nodes;
        this.rmqBrokerConfig = rmqBrokerConfig;
    }

    @Override
    public void setup() {
        try {
            //Download rocketmq package
            log.info("Node {} download rocketmq...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", INSTALL_DIR, INSTALL_DIR));
            SshUtil.execCommandInDir(node, INSTALL_DIR,
                String.format("curl https://archive.apache.org/dist/rocketmq/%s/rocketmq-all-%s-bin-release.zip -o rocketmq.zip", ROCKETMQ_VERSION, ROCKETMQ_VERSION),
                "unzip rocketmq.zip", "rm -f rocketmq.zip", "mv rocketmq-all*/* .", "rmdir rocketmq-all*");
            log.info("Node {} download rocketmq success", node);

            //For docker test, because the memory of local computer is too small
            SshUtil.execCommandInDir(node, INSTALL_DIR, "sed -i 's/-Xms8g -Xmx8g -Xmn4g/-Xmx1500m/g' bin/runbroker.sh");
            SshUtil.execCommandInDir(node, INSTALL_DIR, "sed -i  's/-Xms4g -Xmx4g -Xmn2g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m/-Xms500m -Xmx500m -Xmn250m -XX:MetaspaceSize=16m -XX:MaxMetaspaceSize=40m/g' bin/runserver.sh");
            SshUtil.execCommandInDir(node, INSTALL_DIR, "sed -i 's/exit -1/exit 0/g' bin/mqshutdown");

            //Prepare broker conf
            Field[] fields = rmqBrokerConfig.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                String value = (String) fields[i].get(rmqBrokerConfig);
                if (value != null && !value.isEmpty()) {
                    SshUtil.execCommandInDir(node, INSTALL_DIR, String.format("echo '%s' >> broker-chaos-test.conf", name + "=" + value));
                }
            }

            String dledgerPeers = getDledgerPeers(nodes);

            SshUtil.execCommandInDir(node, INSTALL_DIR, String.format("echo '%s' >> broker-chaos-test.conf", "dLegerPeers=" + dledgerPeers));

            SshUtil.execCommandInDir(node, INSTALL_DIR, String.format("echo '%s' >> broker-chaos-test.conf", "dLegerSelfId=n" + nodes.indexOf(node)));

        } catch (Exception e) {
            log.error("Node {} setup rocketmq node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void teardown() {
        try {
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", INSTALL_DIR, INSTALL_DIR));
        } catch (Exception e) {
            log.error("Node {} teardown rocketmq node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        try {
            //Start nameserver
            if (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty()) {
                log.info("Node {} start nameserver...", node);
                SshUtil.execCommandInDir(node, INSTALL_DIR, "nohup sh bin/mqnamesrv > nameserver.log 2>&1 &");
            }
            //Start broker
            log.info("Node {} start broker...", node);
            SshUtil.execCommandInDir(node, INSTALL_DIR, String.format("nohup sh bin/mqbroker -n '%s' -c broker-chaos-test.conf > broker.log 2>&1 &"
                , getNameserver(nodes)));
        } catch (Exception e) {
            log.error("Node {} start rocketmq node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        kill();
    }

    @Override
    public void kill() {
        try {
            //Kill nameserver
            if (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty()) {
                log.info("Node {} nameserver killed...", node);
                SshUtil.execCommandInDir(node, INSTALL_DIR, "sh bin/mqshutdown namesrv");
            }
            //Kill broker
            log.info("Node {} broker killed...", node);
            SshUtil.execCommandInDir(node, INSTALL_DIR, "sh bin/mqshutdown broker");
        } catch (Exception e) {
            log.error("Node {} kill rocketmq node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        List<String> names = new ArrayList<>();
        names.add("org.apache.rocketmq.broker.BrokerStartup");
        if (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty()) {
            names.add("org.apache.rocketmq.namesrv.NamesrvStartup");
        }
        try {
            PauseProcessUtil.suspend(node, names);
        } catch (Exception e) {
            log.error("Node {} pause rocketmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        List<String> names = new ArrayList<>();
        names.add("org.apache.rocketmq.broker.BrokerStartup");
        if (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty()) {
            names.add("org.apache.rocketmq.namesrv.NamesrvStartup");
        }
        try {
            PauseProcessUtil.recover(node, names);
        } catch (Exception e) {
            log.error("Node {} resume rocketmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    private String getDledgerPeers(List<String> nodes) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            res.append("n" + i + "-" + nodes.get(i) + ":20911;");
        }
        return res.toString();
    }

    private String getNameserver(List<String> nodes) {
        if (rmqBrokerConfig.namesrvAddr != null && !rmqBrokerConfig.namesrvAddr.isEmpty()) {
            return rmqBrokerConfig.namesrvAddr;
        } else {
            StringBuilder res = new StringBuilder();
            nodes.forEach(node -> res.append(node + ":9876;"));

            return res.toString();
        }
    }

}
