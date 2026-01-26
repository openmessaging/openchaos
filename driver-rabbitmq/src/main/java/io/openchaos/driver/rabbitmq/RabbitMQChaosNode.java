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
package io.openchaos.driver.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.queue.QueueNode;
import io.openchaos.driver.rabbitmq.config.RabbitMQBrokerConfig;
import io.openchaos.driver.rabbitmq.config.RabbitMQConfig;
import io.openchaos.driver.rabbitmq.core.ClusterStatus;
import io.openchaos.driver.rabbitmq.core.Sync;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class RabbitMQChaosNode implements QueueNode {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosNode.class);
    private static final String PROCESS = "beam.smp";

    private final String node;
    private final List<String> nodes;
    private final Sync sync;
    private final String rmqHome;
    private String rabbitmqVersion = "4.2.3";

    public RabbitMQChaosNode(String node, List<String> nodes, RabbitMQConfig config, RabbitMQBrokerConfig brokerConfig, Sync sync) {
        this.node = node;
        this.nodes = nodes;
        this.sync = sync;
        if (StringUtils.isNotBlank(config.rabbitmqVersion)) this.rabbitmqVersion = config.rabbitmqVersion;
        this.rmqHome = "/usr/local/rabbitmq-server-" + rabbitmqVersion;
    }

    @Override
    public void setup() {
        if (sync.status == Sync.State.START || sync.status == Sync.State.FINISH) return;

        sync.status = Sync.State.START;
        Executor executor = new ForkJoinPool(nodes.size());
        CountDownLatch latch = new CountDownLatch(nodes.size());

        nodes.forEach(no -> executor.execute(() -> {
            try {
                setupNode(no);
            } finally {
                latch.countDown();
            }
        }));

        try {
            latch.await(20, TimeUnit.MINUTES);
            sync.addUser("root", "root");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            sync.status = Sync.State.FINISH;
        }
    }

    private void setupNode(String no) {
        try {
            installErlang(no);
            installRabbitmq(no);

            sync.barrier.await(14, TimeUnit.MINUTES);
            sync.resetBarrier();
            sync.syncCookie(no);
            sync.barrier.await(5, TimeUnit.MINUTES);

            // Clean start
            SshUtil.execCommand(no, "killall -q " + PROCESS + " || true");
            SshUtil.execCommand(no, "rm -rf " + rmqHome + "/var/lib/rabbitmq/mnesia/*");
            Thread.sleep(2500);
            SshUtil.execCommand(no, "rabbitmq-server -detached");
            Thread.sleep(5000);

            sync.resetBarrier();
            boolean isLeader = Objects.equals(no, sync.getLeader());

            // App Reset Logic
            log.info("Resetting RabbitMQ on node: {}", no);
            SshUtil.execCommand(no, "rabbitmqctl stop_app");
            Thread.sleep(10000);
            SshUtil.execCommand(no, "rabbitmqctl reset");

            if (!isLeader) {
                Thread.sleep(15000); // Wait for leader
                SshUtil.execCommand(no, "rabbitmqctl join_cluster rabbit@" + sync.getLeader());
            }

            SshUtil.execCommand(no, "rabbitmqctl start_app");
            Thread.sleep(15000);

            waitForCluster(no);
        } catch (Exception e) {
            log.error("Setup failed for node {}", no, e);
            throw new RuntimeException(e);
        }
    }

    private void waitForCluster(String no) throws Exception {
        sync.barrier.await(5, TimeUnit.MINUTES);
        sync.resetBarrier();
        ObjectMapper mapper = new ObjectMapper();
        while (true) {
            String res = SshUtil.execCommandWithArgsReturnStr(no, "rabbitmqctl cluster_status --formatter json");
            ClusterStatus status = mapper.readValue(res, ClusterStatus.class);
            if (status != null && status.getRunningNodes().size() == nodes.size()) break;
            Thread.sleep(5000);
        }
        sync.barrier.await(5, TimeUnit.MINUTES);
    }

    private void installErlang(String no) throws Exception {
        if (StringUtils.isNotBlank(safeExec(no, "which erl"))) {
            SshUtil.execCommand(no, "pgrep epmd || sudo epmd -daemon");
            return;
        }
        SshUtil.execCommand(no, "apt update && apt install -y erlang vim make libtool libevent-dev lua5.3 libssl-dev flex gcc g++ ncurses-dev wget lrzsz xz-utils");
        SshUtil.execCommand(no, "pgrep epmd || sudo epmd -daemon");
    }

    private void installRabbitmq(String no) throws Exception {
        if (StringUtils.isNotBlank(safeExec(no, "which rabbitmq-server"))) return;

        String tar = "rabbitmq-server-generic-unix-" + rabbitmqVersion + ".tar";
        String xz = tar + ".xz";

        if (!StringUtils.contains(safeExec(no, "ls"), tar)) {
            SshUtil.execCommand(no, "wget https://github.com/rabbitmq/rabbitmq-server/releases/download/v" + rabbitmqVersion + "/" + xz);
            SshUtil.execCommand(no, "xz -d " + xz);
        }

        SshUtil.execCommand(no, String.format("tar -xvf %s && rm -rf %s && mv rabbitmq_server-%s %s", tar, rmqHome, rabbitmqVersion, rmqHome));

        String pathCmd = "export PATH=$PATH:" + rmqHome + "/sbin";
        SshUtil.execCommand(no, String.format("echo '%s' >> /etc/profile && echo '%s' >> ~/.bashrc", pathCmd, pathCmd));
        SshUtil.execCommand(no, "rabbitmq-plugins enable rabbitmq_management");
    }

    private String safeExec(String no, String cmd) {
        try {
            return SshUtil.execCommandWithArgsReturnStr(no, cmd);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void start() {
        try {
            SshUtil.execCommand(node, rmqHome + "/sbin/rabbitmq-server -detached");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void teardown() {
        stop();
    }

    @Override
    public void stop() {
        try {
            KillProcessUtil.kill(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop rabbitmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void kill() {
        try {
            KillProcessUtil.forceKillInErl(node, PROCESS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        try {
            PauseProcessUtil.suspend(node, PROCESS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        try {
            PauseProcessUtil.resumeInErl(node, PROCESS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}