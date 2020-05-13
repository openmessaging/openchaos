package io.openmessaging.chaos.driver.kafka;

import io.openmessaging.chaos.common.Message;
import io.openmessaging.chaos.driver.mq.MQChaosPullConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class KafkaChaosPullConsumer implements MQChaosPullConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaChaosPullConsumer.class);
    private KafkaConsumer kafkaConsumer;

    public KafkaChaosPullConsumer(KafkaConsumer kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    @Override
    public List<Message> dequeue() {
        log.info("consumer message.....");
        try {
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(5000);
                if (!records.isEmpty()) {

                    List<Message> messageList = new ArrayList<>();
                    for (ConsumerRecord<String, String> record : records) {
                        messageList.add(new Message(record.key(), record.value().getBytes()));
                    }
                    return messageList;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("dequeue error", e);
        }
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }
}
