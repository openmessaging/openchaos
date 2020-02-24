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

package io.openmessaging.chaos.driver.rocketmq;

import com.google.common.collect.Lists;
import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.driver.MQChaosClient;
import java.util.List;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQChaosClient implements MQChaosClient {

    private final DefaultMQProducer defaultMQProducer;

    private final DefaultLitePullConsumer defaultLitePullConsumer;

    private static final Logger logger = LoggerFactory.getLogger(RocketMQChaosClient.class);

    private String chaosTopic;

    public RocketMQChaosClient(final DefaultMQProducer defaultMQProducer,
        final DefaultLitePullConsumer defaultLitePullConsumer, String chaosTopic) {
        this.defaultMQProducer = defaultMQProducer;
        this.defaultLitePullConsumer = defaultLitePullConsumer;
        this.chaosTopic = chaosTopic;
    }

    public InvokeResult enqueue(String value) {
        Message message = new Message(chaosTopic, value.getBytes());
        try {
            defaultMQProducer.send(message);
        } catch (RemotingException e) {
            if (e instanceof RemotingConnectException || e instanceof RemotingSendRequestException) {
                logger.warn("enqueue fail", e);
                return InvokeResult.FAIL;
            } else {
                logger.warn("enqueue unknown", e);
                return InvokeResult.UNKNOWN;
            }
        } catch (IllegalStateException | MQClientException | InterruptedException | MQBrokerException e) {
            logger.warn("enqueue fail", e);
            return InvokeResult.FAIL;
        } catch (Exception e) {
            logger.warn("enqueue unknown", e);
            return InvokeResult.UNKNOWN;
        }
        return InvokeResult.SUCCESS;
    }

    public List<String> dequeue() {
        List<MessageExt> messages = defaultLitePullConsumer.poll();
        if (!messages.isEmpty()) {
            defaultLitePullConsumer.commitSync();
            return Lists.transform(messages, messageExt -> new String(messageExt.getBody()));
        } else {
            return null;
        }
    }

    public void close() {
        if (defaultMQProducer != null) {
            defaultMQProducer.shutdown();
        }
        if (defaultLitePullConsumer != null) {
            defaultLitePullConsumer.shutdown();
        }
    }
}
