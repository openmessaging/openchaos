/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openchaos.driver.rocketmq;

import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.queue.QueueNode;
import io.openchaos.driver.rocketmq.config.RocketMQBrokerConfig;
import io.openchaos.driver.rocketmq.config.RocketMQConfig;
import java.lang.reflect.Field;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQChaosNode implements QueueNode {

    private static final String BROKER_PROCESS_NAME = "BrokerStartup";
    private static final String NAMESERVER_PROCESS_NAME = "NamesrvStartup";
    private static final Logger log = LoggerFactory.getLogger(RocketMQChaosNode.class);
    private String node;
    private List<String> nodes;
    private List<String> metaNodes;
    private RocketMQBrokerConfig rmqBrokerConfig;
    private String installDir = "rocketmq-chaos-test";
    private String rocketmqVersion = "4.6.0";
    private String configureFilePath = "broker-chaos-test.conf";
    private String nameServerPort = "9876";
    
    public RocketMQChaosNode(String node, List<String> nodes, List<String> metaNodes, RocketMQConfig rmqConfig,
        RocketMQBrokerConfig rmqBrokerConfig) {
        this.node = node;
        this.nodes = nodes;
        this.metaNodes = metaNodes;
        this.rmqBrokerConfig = rmqBrokerConfig;
        if (rmqConfig.installDir != null && !rmqConfig.installDir.isEmpty()) {
            this.installDir = rmqConfig.installDir;
        }
        if (rmqConfig.rocketmqVersion != null && !rmqConfig.rocketmqVersion.isEmpty()) {
            this.rocketmqVersion = rmqConfig.rocketmqVersion;
        }
        if (rmqConfig.nameServerPort != null && !rmqConfig.nameServerPort.isEmpty()) {
            this.nameServerPort = rmqConfig.nameServerPort;
        }
        if (rmqConfig.configureFilePath != null && !rmqConfig.configureFilePath.isEmpty()) {
            this.configureFilePath = rmqConfig.configureFilePath;
        }
    }

    @Override
    public void setup() {
        try {
            //Download rocketmq package
            log.info("Node {} download rocketmq...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                String.format("curl https://archive.apache.org/dist/rocketmq/%s/rocketmq-all-%s-bin-release.zip -o rocketmq.zip", rocketmqVersion, rocketmqVersion),
                "unzip rocketmq.zip", "rm -f rocketmq.zip", "mv rocketmq-all*/* .", "rmdir rocketmq-all*");
            log.info("Node {} download rocketmq success", node);

            //For docker test, because the memory of local computer is too small
            SshUtil.execCommandInDir(node, installDir, "sed -i 's/-Xms8g -Xmx8g -Xmn4g/-Xmx1500m/g' bin/runbroker.sh");
            SshUtil.execCommandInDir(node, installDir, "sed -i  's/-Xms4g -Xmx4g -Xmn2g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m/-Xms500m -Xmx500m -Xmn250m -XX:MetaspaceSize=16m -XX:MaxMetaspaceSize=40m/g' bin/runserver.sh");

            //Prepare broker conf
            Field[] fields = rmqBrokerConfig.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                String value = (String) fields[i].get(rmqBrokerConfig);
                if (value != null && !value.isEmpty()) {
                    SshUtil.execCommandInDir(node, installDir, String.format("echo '%s' >> %s", name + "=" + value, configureFilePath));
                }
            }

            String dledgerPeers = getDledgerPeers(nodes);

            SshUtil.execCommandInDir(node, installDir, String.format("echo '%s' >> %s", "dLegerPeers=" + dledgerPeers, configureFilePath));
            SshUtil.execCommandInDir(node, installDir, String.format("echo '%s' >> %s", "dLegerSelfId=n" + nodes.indexOf(node), configureFilePath));

        } catch (Exception e) {
            log.error("Node {} setup rocketmq node failed", node, e);
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
            //Start nameserver
            if ((metaNodes == null || metaNodes.isEmpty())
                && (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty())) {
                log.info("Node {} start nameserver...", node);
                SshUtil.execCommandInDir(node, installDir, "nohup sh bin/mqnamesrv > nameserver.log 2>&1 &");
            }
            //Start broker
            log.info("Node {} start broker...", node);
            SshUtil.execCommandInDir(node, installDir, String.format("nohup sh bin/mqbroker -n '%s' -c %s > broker.log 2>&1 &"
                , getNameserver(nodes), configureFilePath));
        } catch (Exception e) {
            log.error("Node {} start rocketmq node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            KillProcessUtil.kill(node, BROKER_PROCESS_NAME);
            if ((metaNodes == null || metaNodes.isEmpty())
                && (metaNodes == null && (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty()))) {
                KillProcessUtil.kill(node, NAMESERVER_PROCESS_NAME);
            }
        } catch (Exception e) {
            log.error("Node {} stop rocketmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void kill() {
        try {
            KillProcessUtil.forceKill(node, BROKER_PROCESS_NAME);
            if ((metaNodes == null || metaNodes.isEmpty())
                && (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty())) {
                KillProcessUtil.forceKill(node, NAMESERVER_PROCESS_NAME);
            }
        } catch (Exception e) {
            log.error("Node {} kill rocketmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        try {
            PauseProcessUtil.suspend(node, BROKER_PROCESS_NAME);
            if ((metaNodes == null || metaNodes.isEmpty())
                && (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty())) {
                PauseProcessUtil.suspend(node, NAMESERVER_PROCESS_NAME);
            }
        } catch (Exception e) {
            log.error("Node {} pause rocketmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        try {
            PauseProcessUtil.resume(node, BROKER_PROCESS_NAME);
            if ((metaNodes == null || metaNodes.isEmpty())
                && (rmqBrokerConfig.namesrvAddr == null || rmqBrokerConfig.namesrvAddr.isEmpty())) {
                PauseProcessUtil.resume(node, NAMESERVER_PROCESS_NAME);
            }
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
        } else if (metaNodes != null) {
            StringBuilder res = new StringBuilder();
            metaNodes.forEach(node -> res.append(node + ":" + nameServerPort + ";"));
            return res.toString();
        } else {
            StringBuilder res = new StringBuilder();
            nodes.forEach(node -> res.append(node + ":" + nameServerPort + ";"));
            return res.toString();
        }
    }

}
