/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.openmessaging.chaos.client;

import io.openmessaging.chaos.Recorder;
import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.driver.MQChaosClient;
import io.openmessaging.chaos.driver.MQChaosDriver;
import io.openmessaging.chaos.generator.QueueGenerator;
import io.openmessaging.chaos.generator.QueueOperation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueClient implements Client {

    private static final AtomicInteger clientIdGenerator = new AtomicInteger(0);

    private MQChaosClient mqChaosClient;

    private MQChaosDriver mqChaosDriver;

    private String chaosTopic;

    private Recorder recorder;

    private int clientId;

    private static final Logger logger = LoggerFactory.getLogger(QueueClient.class);

    public QueueClient(MQChaosDriver mqChaosDriver, String chaosTopic, Recorder recorder) {
        this.mqChaosDriver = mqChaosDriver;
        this.chaosTopic = chaosTopic;
        this.recorder = recorder;
        clientId = clientIdGenerator.getAndIncrement();
    }

    public void setup() {
        mqChaosClient = mqChaosDriver.createChaosClient(chaosTopic);
    }

    public void teardown() {
        mqChaosClient.close();
    }

    public void nextInvoke() {

        QueueOperation op = QueueGenerator.generate();

        recorder.recordRequest(clientId, op.getInvokeOperation(), op.getValue());
        if (op.getInvokeOperation().equals("enqueue")) {
            InvokeResult invokeResult = mqChaosClient.enqueue(op.getValue());
            recorder.recordResponse(clientId, op.getInvokeOperation(), invokeResult, op.getValue());
        } else {
            List<String> dequeueList = mqChaosClient.dequeue();
            if (dequeueList == null || dequeueList.isEmpty()) {
                recorder.recordResponse(clientId, op.getInvokeOperation(), InvokeResult.FAILURE, null);
            } else {
                recorder.recordResponse(clientId, op.getInvokeOperation(), InvokeResult.SUCCESS, dequeueList.toString());
            }
        }
    }

    public void lastInvoke() {
        logger.info("Client {} invoke drain", clientId);
        //Drain
        recorder.recordRequest(clientId, "dequeue", null);
        List<String> dequeueList = mqChaosClient.dequeue();
        while (dequeueList != null && !dequeueList.isEmpty()) {
            recorder.recordResponse(clientId, "dequeue", InvokeResult.SUCCESS, dequeueList.toString());
            recorder.recordRequest(clientId, "dequeue", null);
            dequeueList = mqChaosClient.dequeue();
        }
        recorder.recordResponse(clientId, "dequeue", InvokeResult.FAILURE, null);
    }
}
