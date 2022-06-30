package io.openmessaging.driver.rabbitmq;

import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.QueueDriver;
import io.openchaos.driver.queue.QueueProducer;
import io.openchaos.driver.queue.QueuePullConsumer;
import io.openchaos.driver.queue.QueuePushConsumer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RabbitMQDriver implements QueueDriver {
    @Override
    public String getMetaNode() {
        return null;
    }

    @Override
    public String getMetaName() {
        return null;
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public ChaosNode createChaosNode(String node, List<String> nodes) {
        return null;
    }

    @Override
    public String getStateName() {
        return null;
    }

    @Override
    public void createTopic(String topic, int partitions) {

    }

    @Override
    public QueueProducer createProducer(String topic) {
        return null;
    }

    @Override
    public QueuePushConsumer createPushConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        return null;
    }

    @Override
    public QueuePullConsumer createPullConsumer(String topic, String subscriptionName) {
        return null;
    }
}
