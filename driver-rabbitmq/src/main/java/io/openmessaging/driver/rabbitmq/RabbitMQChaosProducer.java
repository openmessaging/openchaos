package io.openmessaging.driver.rabbitmq;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.queue.QueueProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

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
        try{
            producer.sendMessage(queueName, payload);
        } catch (IOException e) {
            log.warn("Enqueue fail");
            return InvokeResult.FAILURE;
        } catch (TimeoutException e) {
            log.warn("Enqueue timeout");
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return InvokeResult.SUCCESS.setExtraInfoAndReturnSelf(new String(payload));
    }

    @Override
    public InvokeResult enqueue(String shardingKey, byte[] payload) {
        return null; //todo 分片队列
    }

    private  boolean notNull(String s){
        return s != null && !s.equals("");
    }
}
