package io.openmessaging.chaos.driver.kafka;

import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.driver.mq.MQChaosProducer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaChaosProducer implements MQChaosProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaChaosProducer.class);
    private String chaosTopic;
    private KafkaProducer kafkaProducer;

    public KafkaChaosProducer(KafkaProducer kafkaProducer, String chaosTopic) {
        this.kafkaProducer = kafkaProducer;
        this.chaosTopic = chaosTopic;
    }

    @Override
    public InvokeResult enqueue(byte[] payload) {
        try {
            log.info("send message.....");
            kafkaProducer.send(new ProducerRecord<String, String>(chaosTopic, new String(payload)));
        } catch (Exception e) {
            log.info("enqueue error", e);
            return InvokeResult.FAILURE;
        }
        return InvokeResult.SUCCESS;
    }

    @Override
    public InvokeResult enqueue(String shardingKey, byte[] payload) {
        try {
            kafkaProducer.send(new ProducerRecord(chaosTopic, shardingKey, payload));
        } catch (Exception e) {
            return InvokeResult.FAILURE;
        }
        return InvokeResult.SUCCESS;
    }

    @Override
    public void start() {
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }
}
