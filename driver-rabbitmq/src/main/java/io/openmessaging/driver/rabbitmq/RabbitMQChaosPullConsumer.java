package io.openmessaging.driver.rabbitmq;

import io.openchaos.common.Message;
import io.openchaos.driver.queue.QueuePullConsumer;

import java.util.List;

public class RabbitMQChaosPullConsumer implements QueuePullConsumer {
    @Override
    public void start() {

    }

    @Override
    public void close() {

    }

    @Override
    public List<Message> dequeue() {
        return null;
    }
}
