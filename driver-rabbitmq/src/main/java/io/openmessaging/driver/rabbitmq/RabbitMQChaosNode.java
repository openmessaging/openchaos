package io.openmessaging.driver.rabbitmq;

import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.queue.QueueNode;
import io.openmessaging.driver.rabbitmq.config.RabbitMQBrokerConfig;
import io.openmessaging.driver.rabbitmq.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class RabbitMQChaosNode implements QueueNode {
    private static final String BROKER_PROCESS_NAME = "rabbitmq";
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosNode.class);
    private String node;
    private List<String> nodes;
    private RabbitMQBrokerConfig rmqBrokerConfig;
    private String installDir = "rabbitmq-chaos-test";
    private String rabbitmqVersion = "3.8.0";
    private String configureFilePath = "broker-chaos-test.conf";

    public RabbitMQChaosNode(String node, List<String> nodes, RabbitMQConfig rmqConfig,
                             RabbitMQBrokerConfig rmqBrokerConfig) {
        this.node = node;
        this.nodes = nodes;
        this.rmqBrokerConfig = rmqBrokerConfig;
        if (rmqConfig.installDir != null && !rmqConfig.installDir.isEmpty()) {
            this.installDir = rmqConfig.installDir;
        }
        if (rmqConfig.rabbitmqVersion != null && !rmqConfig.rabbitmqVersion.isEmpty()) {
            this.rabbitmqVersion = rmqConfig.rabbitmqVersion;
        }
        if (rmqConfig.configureFilePath != null && !rmqConfig.configureFilePath.isEmpty()) {
            this.configureFilePath = rmqConfig.configureFilePath;
        }
    }

    //not recommended use setup method
    @Override
    public void setup() {
        try {
            //Download rocketmq package
            log.info("Node {} download rabbitmq...", node);
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                    String.format("wget https://github.com/rabbitmq/rabbitmq-server/releases/download/v%s/rabbitmq-server-%s-1.el7.noarch.rpm -O rabbitmq.noarch.rpm", rabbitmqVersion, rabbitmqVersion),
                    "rpm -ivh --nodeps rabbitmq.noarch.rpm", "rm -f rabbitmq.noarch.rpm", "rabbitmq-plugins enable rabbitmq_management");
            log.info("Node {} download rabbitmq success", node);
            SshUtil.execCommand(node, "rabbitmqctl start_app");
        } catch (Exception e) {
            log.error("Node {} setup rabbitmq node failed", node, e);
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
            // start broker
            log.info("Node {} start broker...", node);
            SshUtil.execCommandInDir(node, installDir, String.format("nohup sh sbin/rabbitmq-server --detached  > broker.log 2>&1 &"
            ));
        } catch (Exception e) {
            log.error("Node {} start rabbitmq node failed", node, e);
            throw new RuntimeException(e);
        }
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
            KillProcessUtil.forceKill(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} kill rabbitmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        try {
            PauseProcessUtil.suspend(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} pause rabbitmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        try {
            PauseProcessUtil.resume(node, BROKER_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} resume rabbitmq processes failed", node, e);
            throw new RuntimeException(e);
        }
    }
}
