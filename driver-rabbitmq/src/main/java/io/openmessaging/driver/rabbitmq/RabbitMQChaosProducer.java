package io.openmessaging.driver.rabbitmq;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.queue.QueueProducer;

public class RabbitMQChaosProducer implements QueueProducer {
    @Override
    public void start() {

    }

    @Override
    public void close() {

    }

    @Override
    public InvokeResult enqueue(byte[] payload) {
        return null;
    }

    @Override
    public InvokeResult enqueue(String shardingKey, byte[] payload) {
        return null;
    }
}
