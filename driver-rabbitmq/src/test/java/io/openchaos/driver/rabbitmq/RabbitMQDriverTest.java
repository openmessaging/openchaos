package io.openchaos.driver.rabbitmq;

import io.openchaos.common.Message;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.queue.ConsumerCallback;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RabbitMQDriverTest {
    static RabbitMQDriver driver = new RabbitMQDriver();
    static File configurationFile = new File("src/test/resources/rabbitmq.yaml");
    static List<String> nodes = new ArrayList<String>(){{add("tcloud");}};
    static {
        try {
            SshUtil.init("root", "Yigeyy00", new ArrayList<String>(){{add("tcloud");}});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            driver.initialize(configurationFile, nodes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    @Test
    public void shutdown() {
        driver.shutdown();
    }

    @Test
    public void createChaosNode() {
        driver.createChaosNode("tcloud", nodes);
    }

    @Test
    public void getStateName() {
        assertNotNull(driver.getStateName());
    }

    @Test
    public void createTopic() {
        driver.createTopic("", 1);
    }

    @Test
    public void createProducer() {
        driver.createTopic("", 1);
        driver.createProducer("");
    }

    @Test
    public void createPushConsumer() {
        driver.createPushConsumer("", "push_consumer", new ConsumerCallback() {
            @Override
            public void messageReceived(Message message) {

            }
        });
    }

    @Test
    public void createPullConsumer() {
        driver.createPullConsumer("", "pull_consumer");
    }
}