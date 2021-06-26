/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.openchaos.driver.kafka;

import io.openchaos.common.Message;
import io.openchaos.driver.queue.MQChaosPullConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There may be issues during operation, KafkaChaosPushConsumer is recommended
 */
@Deprecated
public class KafkaChaosPullConsumer implements MQChaosPullConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaChaosPullConsumer.class);
    private KafkaConsumer kafkaConsumer;
    private final ExecutorService executor;

    public KafkaChaosPullConsumer(KafkaConsumer kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public List<Message> dequeue() {

        List<Message> res = null;

        Future<List<Message>> consumerTask = executor.submit(new Callable<List<Message>>() {
            @Override public List<Message> call() throws Exception {
                ConsumerRecords<String, byte[]> records = kafkaConsumer.poll(500);
                if (!records.isEmpty()) {
                    List<Message> messageList = new ArrayList<>();
                    for (ConsumerRecord<String, byte[]> record : records) {
                        messageList.add(new Message(record.key(), record.value()));
                    }
                    return messageList;
                } else {
                    return null;
                }
            }
        });
        try {
            res = consumerTask.get();
        } catch (Exception e) {
            log.error("dequeue error", e);
        }
        return res;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {
        try {
            executor.submit(() -> {
                if (kafkaConsumer != null) {
                    kafkaConsumer.close();
                }
            }).get();
        } catch (Exception e) {
            log.error("Close KafkaChaosPullConsumer error", e);
        }
        executor.shutdown();
    }
}
