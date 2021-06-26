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

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.queue.MQChaosProducer;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaChaosProducer implements MQChaosProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaChaosProducer.class);
    private String chaosTopic;
    private KafkaProducer<String, byte[]> kafkaProducer;

    public KafkaChaosProducer(KafkaProducer<String, byte[]> kafkaProducer, String chaosTopic) {
        this.kafkaProducer = kafkaProducer;
        this.chaosTopic = chaosTopic;
    }

    @Override
    public InvokeResult enqueue(byte[] payload) {
        try {
            kafkaProducer.send(new ProducerRecord<>(chaosTopic, payload)).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("enqueue timeout...", e);
                return InvokeResult.UNKNOWN;
            }
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            log.warn("enqueue error", e);
            return InvokeResult.FAILURE;
        }

        return InvokeResult.SUCCESS;
    }

    @Override
    public InvokeResult enqueue(String shardingKey, byte[] payload) {
        try {
            kafkaProducer.send(new ProducerRecord<>(chaosTopic, shardingKey, payload)).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("enqueue timeout...", e);
                return InvokeResult.UNKNOWN;
            }
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            log.warn("enqueue error", e);
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
