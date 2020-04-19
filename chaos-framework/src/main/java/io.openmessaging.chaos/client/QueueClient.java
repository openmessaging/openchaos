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

package io.openmessaging.chaos.client;

import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.common.Message;
import io.openmessaging.chaos.driver.mq.MQChaosClient;
import io.openmessaging.chaos.driver.mq.MQChaosDriver;
import io.openmessaging.chaos.generator.QueueGenerator;
import io.openmessaging.chaos.generator.QueueOperation;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.recorder.RequestLogEntry;
import io.openmessaging.chaos.recorder.ResponseLogEntry;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueClient implements Client {

    private static final AtomicInteger clientIdGenerator = new AtomicInteger(0);
    private static final Logger log = LoggerFactory.getLogger(QueueClient.class);
    private MQChaosClient mqChaosClient;
    private MQChaosDriver mqChaosDriver;
    private String chaosTopic;
    private Recorder recorder;
    private int clientId;
    private boolean isOrderTest;
    private List<String> shardingKeys;
    private Random random = new Random();

    public QueueClient(MQChaosDriver mqChaosDriver, String chaosTopic, Recorder recorder, boolean isOrderTest,
        List<String> shardingKeys) {
        this.mqChaosDriver = mqChaosDriver;
        this.chaosTopic = chaosTopic;
        this.recorder = recorder;
        clientId = clientIdGenerator.getAndIncrement();
        this.isOrderTest = isOrderTest;
        this.shardingKeys = shardingKeys;
    }

    public void setup() {
        mqChaosClient = mqChaosDriver.createChaosClient(chaosTopic);
        mqChaosClient.start();
    }

    public void teardown() {
        mqChaosClient.close();
    }

    public void nextInvoke() {
        QueueOperation op = QueueGenerator.generate();
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, op.getInvokeOperation(), op.getValue(), System.currentTimeMillis());

        if (op.getInvokeOperation().equals("enqueue")) {
            InvokeResult invokeResult;
            if (isOrderTest) {
                String shardingKey = shardingKeys.get(random.nextInt(shardingKeys.size()));
                requestLogEntry.shardingKey = shardingKey;
                recorder.recordRequest(requestLogEntry);
                invokeResult = mqChaosClient.enqueue(shardingKey, op.getValue());
                recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                    invokeResult, shardingKey, op.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
            } else {
                recorder.recordRequest(requestLogEntry);
                invokeResult = mqChaosClient.enqueue(op.getValue());
                recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                    invokeResult, op.getValue(), System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
            }
        } else {
            recorder.recordRequest(requestLogEntry);
            List<Message> dequeueList = mqChaosClient.dequeue();
            if (dequeueList == null || dequeueList.isEmpty()) {
                recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                    InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
            } else {
                for (Message msg : dequeueList) {
                    recorder.recordResponse(new ResponseLogEntry(clientId, op.getInvokeOperation(),
                        InvokeResult.SUCCESS, msg.shardingKey, msg.payload, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
                }
            }
        }
    }

    public void lastInvoke() {
        log.info("Client {} invoke drain", clientId);
        //Drain
        RequestLogEntry requestLogEntry = new RequestLogEntry(clientId, "dequeue", null, System.currentTimeMillis());
        recorder.recordRequest(requestLogEntry);
        List<Message> dequeueList = mqChaosClient.dequeue();
        while (dequeueList != null && !dequeueList.isEmpty()) {
            for (Message msg : dequeueList) {
                recorder.recordResponse(new ResponseLogEntry(clientId, "dequeue", InvokeResult.SUCCESS, msg.shardingKey, msg.payload, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
            }
            requestLogEntry = new RequestLogEntry(clientId, "dequeue", null, System.currentTimeMillis());
            recorder.recordRequest(requestLogEntry);
            dequeueList = mqChaosClient.dequeue();
        }
        recorder.recordResponse(new ResponseLogEntry(clientId, "dequeue", InvokeResult.FAILURE, null, System.currentTimeMillis(), System.currentTimeMillis() - requestLogEntry.timestamp));
    }
}
