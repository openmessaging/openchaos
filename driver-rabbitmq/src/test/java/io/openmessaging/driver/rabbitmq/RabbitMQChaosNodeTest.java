package io.openmessaging.driver.rabbitmq;

import io.openchaos.common.utils.SshUtil;
import io.openmessaging.driver.rabbitmq.config.RabbitMQConfig;
import org.junit.Test;

import javax.xml.soap.Node;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class RabbitMQChaosNodeTest {
    static RabbitMQChaosNode chaosNode;


    static {
        RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();
        rabbitMQConfig.rabbitmqVersion = "3.3.5";
        rabbitMQConfig.setInstallDir("/usr");
        try {
            SshUtil.init("root", "Yigeyy00", new ArrayList<String>(){{add("acloud");}});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        chaosNode = new RabbitMQChaosNode("acloud",
                new ArrayList<String>(){{add("acloud");}}, rabbitMQConfig, null);
    }

    @Test
    public void teardown() {
        chaosNode.start();
        chaosNode.teardown();
    }

    @Test
    public void start() {
        chaosNode.start();
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