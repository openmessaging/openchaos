package io.openchaos.driver.rabbitmq;

import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.rabbitmq.config.RabbitMQConfig;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class RabbitMQChaosNodeTest {
    static RabbitMQChaosNode chaosNode;


    static {
        RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();
        rabbitMQConfig.rabbitmqVersion = "3.8.4";
        rabbitMQConfig.setInstallDir("/usr");
        try {
            SshUtil.init("root", "Yigeyy00", new ArrayList<String>(){{add("tcloud");}});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        chaosNode = new RabbitMQChaosNode("tcloud",
                new ArrayList<String>(){{add("tcloud");}}, rabbitMQConfig, null);
    }

    @Test
    public void teardown() {
        chaosNode.start();
        chaosNode.teardown();
    }

    @Test
    public void start() throws Exception {
        chaosNode.start();
        assertNotEquals("", SshUtil.execCommandWithArgsReturnStr("tcloud", String.format("ps ax | grep -i '%s'| grep -v grep | awk '{print $1}'", "beam.smp")).trim());
    }

    @Test
    public void stop() {
        chaosNode.stop();
    }

    @Test
    public void kill() {
        chaosNode.kill();

    }

    @Test
    public void pause() {
        chaosNode.pause();
    }

    @Test
    public void resume() {
        chaosNode.resume();
    }
}