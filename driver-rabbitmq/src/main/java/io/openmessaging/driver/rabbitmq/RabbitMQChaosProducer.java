package io.openmessaging.driver.rabbitmq;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.queue.QueueProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitMQChaosProducer implements QueueProducer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosProducer.class);
    String queueName = "openchaos_client_1";
    private DefaultRabbitMQProducer producer;

    public RabbitMQChaosProducer() {
        producer = new DefaultRabbitMQProducer();
    }

    public RabbitMQChaosProducer(String host, int port, String user, String password, String queueName){
        if (notNull(queueName)) {
            this.queueName = queueName;
        }
        producer = new DefaultRabbitMQProducer(host, port, user, password);
    }

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

    private  boolean notNull(String s){
        return s != null && !s.equals("");
    }
}
