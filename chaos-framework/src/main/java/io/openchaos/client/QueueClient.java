/*
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

package io.openchaos.client;

import io.openchaos.common.InvokeResult;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.QueueDriver;
import io.openchaos.driver.queue.QueueProducer;
import io.openchaos.driver.queue.QueuePullConsumer;
import io.openchaos.driver.queue.QueuePushConsumer;
import io.openchaos.recorder.ResponseLogEntry;
import io.openchaos.generator.SequenceGenerator;
import io.openchaos.generator.Operation;
import io.openchaos.recorder.Recorder;
import io.openchaos.recorder.RequestLogEntry;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueClient implements Client, ConsumerCallback {

    private static final AtomicInteger CLIENT_ID_GENERATOR = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(QueueClient.class);
    private static final String SUBSCRIPTION_NAME = "ChaosTest_ConsumerGroup";
    private QueueProducer producer;
    private QueuePullConsumer pullConsumer;
    private QueuePushConsumer pushConsumer;
    private final QueueDriver pubSubDriver;
    private final String chaosTopic;
    private final Recorder recorder;
    private final int clientId;
    private final boolean isOrderTest;
    private final boolean isUsePull;
    private final List<String> shardingKeys;
    private final AtomicLong msgReceivedCount;
    private final Random random = new Random();

    public QueueClient(QueueDriver pubSubDriver, String chaosTopic, Recorder recorder, boolean isOrderTest,
                       boolean isUsePull, List<String> shardingKeys, AtomicLong msgReceivedCount) {
        this.pubSubDriver = pubSubDriver;
        this.chaosTopic = chaosTopic;
        this.recorder = recorder;
        clientId = CLIENT_ID_GENERATOR.getAndIncrement();
        this.isOrderTest = isOrderTest;
        this.isUsePull = isUsePull;
        this.shardingKeys = shardingKeys;
        this.msgReceivedCount = msgReceivedCount;
    }

    public void setup() {
        producer = pubSubDriver.createProducer(chaosTopic);
        producer.start();
        if (isUsePull) {
            pullConsumer = pubSubDriver.createPullConsumer(chaosTopic, SUBSCRIPTION_NAME);
            pullConsumer.start();
        } else {
            pushConsumer = pubSubDriver.createPushConsumer(chaosTopic, SUBSCRIPTION_NAME, this);
            pushConsumer.start();
        }
    }

    public void teardown() {
        producer.close();
        if (pullConsumer != null) {
            pullConsumer.close();
        }
        if (pushConsumer != null) {
            pushConsumer.close();
        }
    }

    public void nextInvoke() {
        Operation op = SequenceGenerator.generateQueueOperation(isUsePull);
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, op.getInvokeOperation(), op.getValue(), System.currentTimeMillis());
        if (op.getInvokeOperation().equals("enqueue")) {
            InvokeResult invokeResult;
            if (isOrderTest) {
                String shardingKey = shardingKeys.get(random.nextInt(shardingKeys.size()));
                requestLogEntry.shardingKey = shardingKey;
                recorder.recordRequest(requestLogEntry);
                invokeResult = producer.enqueue(shardingKey, op.getValue().getBytes());
                recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                    invokeResult, shardingKey, op.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp, invokeResult.getExtraInfo()));
            } else {
                recorder.recordRequest(requestLogEntry);
                invokeResult = producer.enqueue(op.getValue().getBytes());
                recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                    invokeResult, op.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp, invokeResult.getExtraInfo()));
            }
        } else {
            recorder.recordRequest(requestLogEntry);
            List<Message> dequeueList = pullConsumer.dequeue();
            if (dequeueList == null || dequeueList.isEmpty()) {
                recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                    InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
            } else {
                for (Message msg : dequeueList) {
                    msgReceivedCount.incrementAndGet();
                    recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                        InvokeResult.SUCCESS, msg.shardingKey, new String(msg.payload), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp,
                        msg.extraInfo, msg.receiveTimestamp - msg.sendTimestamp));
                }
            }
        }
    }

    public void lastInvoke() {
        if (isUsePull) {
            log.info("Client {} invoke drain", clientId);
            //Drain
            RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, "dequeue", null, System.currentTimeMillis());
            recorder.recordRequest(requestLogEntry);
            List<Message> dequeueList = pullConsumer.dequeue();
            while (dequeueList != null && !dequeueList.isEmpty()) {
                for (Message msg : dequeueList) {
                    msgReceivedCount.incrementAndGet();
                    recorder.recordResponse(new ResponseLogEntry(clientId, "dequeue", InvokeResult.SUCCESS, msg.shardingKey, new String(msg.payload),
                        System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp, msg.extraInfo, msg.receiveTimestamp - msg.sendTimestamp));
                }
                requestLogEntry = new RequestLogEntry(clientId, "dequeue", null, System.currentTimeMillis());
                recorder.recordRequest(requestLogEntry);
                dequeueList = pullConsumer.dequeue();
            }
            recorder.recordResponse(new ResponseLogEntry(clientId, "dequeue", InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
        }
    }

    @Override
    public void messageReceived(Message message) {
        msgReceivedCount.incrementAndGet();
        recorder.recordResponse(new ResponseLogEntry(clientId, "dequeue", InvokeResult.SUCCESS, message.shardingKey, new String(message.payload), System.currentTimeMillis(), 0,
            message.extraInfo, message.receiveTimestamp - message.sendTimestamp));
    }
}
