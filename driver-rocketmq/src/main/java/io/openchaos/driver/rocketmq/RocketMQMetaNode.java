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
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.rocketmq.config.RocketMQConfig;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nodes for metanode
 */
public class RocketMQMetaNode implements MetaNode {

    private static final String METANODE_PROCESS_NAME = "ControllerStartup";
    private static final Logger log = LoggerFactory.getLogger(RocketMQMetaNode.class);
    private String installDir = "rocketmq-chaos-test";
    private String rocketmqVersion = "4.6.0";
    private String node;
    private List<String> nodes;
    private RocketMQConfig rmqConfig;

    public RocketMQMetaNode(String node, List<String> nodes, RocketMQConfig rmqConfig) {
        this.node = node;
        this.nodes = nodes;
        this.rmqConfig = rmqConfig;
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
            log.info("Node {} download rocketmq for metanode ...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                String.format("curl https://archive.apache.org/dist/rocketmq/%s/rocketmq-all-%s-bin-release.zip -o rocketmq.zip", rocketmqVersion, rocketmqVersion),
                "unzip rocketmq.zip", "rm -f rocketmq.zip", "mv rocketmq-all*/* .", "rmdir rocketmq-all*");
            log.info("Node {} download rocketmq for metanode success", node);

            //For docker test, because the memory of local computer is too small
            SshUtil.execCommandInDir(node, installDir, "sed -i  's/-Xms4g -Xmx4g -Xmn2g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m/-Xms500m -Xmx500m -Xmn250m -XX:MetaspaceSize=16m -XX:MaxMetaspaceSize=40m/g' bin/runserver.sh");
        } catch (Exception e) {
            log.error("Node {} setup metanode node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void teardown() {
        stop();
    }

    @Override public void start() {
        try {
            //Start metanode
            log.info("Node {} start controller...", node);
            SshUtil.execCommandInDir(node, installDir, "source /etc/profile", rmqConfig.metaNodeProcessStartupCommandLine);
        } catch (Exception e) {
            log.error("Node {} start metanode node failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void stop() {
        try {
            KillProcessUtil.kill(node, METANODE_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop metanode processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void kill() {
        try {
            KillProcessUtil.forceKill(node, METANODE_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} kill metanode processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void pause() {
        try {
            PauseProcessUtil.suspend(node, METANODE_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} suspend metanode processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override public void resume() {
        try {
            PauseProcessUtil.resume(node, METANODE_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} resume metanode processes failed", node, e);
            throw new RuntimeException(e);
        }
    }
}
