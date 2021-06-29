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

package io.openchaos.driver.rocketmq;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.queue.MQChaosProducer;
import java.util.List;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQChaosProducer implements MQChaosProducer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQChaosProducer.class);
    private final DefaultMQProducer defaultMQProducer;
    private String chaosTopic;

    public RocketMQChaosProducer(final DefaultMQProducer defaultMQProducer, String chaosTopic) {
        this.defaultMQProducer = defaultMQProducer;
        this.chaosTopic = chaosTopic;
    }

    @Override
    public InvokeResult enqueue(byte[] payload) {
        Message message = new Message(chaosTopic, payload);
        SendResult sendResult = null;
        try {
            sendResult = defaultMQProducer.send(message);
        } catch (RemotingException e) {
            if (e instanceof RemotingConnectException || e instanceof RemotingSendRequestException) {
                log.warn("Enqueue fail", e);
                return InvokeResult.FAILURE;
            } else {
                log.warn("Enqueue unknown", e);
                return InvokeResult.UNKNOWN;
            }
        } catch (IllegalStateException | MQClientException | InterruptedException | MQBrokerException e) {
            log.warn("Enqueue fail", e);
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            log.warn("Enqueue unknown", e);
            return InvokeResult.UNKNOWN;
        }
        return InvokeResult.SUCCESS.setExtraInfoAndReturnSelf(sendResult.toString());
    }

    @Override
    public InvokeResult enqueue(String shardingKey, byte[] payload) {
        Message message = new Message(chaosTopic, payload);
        message.setKeys(shardingKey);
        SendResult sendResult = null;
        try {
            sendResult = defaultMQProducer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    String key = (String) arg;
                    int index = Math.abs(key.hashCode()) % mqs.size();
                    return mqs.get(index);
                }
            }, shardingKey);
        } catch (RemotingException e) {
            if (e instanceof RemotingConnectException || e instanceof RemotingSendRequestException) {
                log.warn("Enqueue fail", e);
                return InvokeResult.FAILURE;
            } else {
                log.warn("Enqueue unknown", e);
                return InvokeResult.UNKNOWN;
            }
        } catch (IllegalStateException | MQClientException | InterruptedException | MQBrokerException e) {
            log.warn("Enqueue fail", e);
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            log.warn("Enqueue unknown", e);
            return InvokeResult.UNKNOWN;
        }
        return InvokeResult.SUCCESS.setExtraInfoAndReturnSelf(sendResult.toString());
    }

    @Override
    public void start() {
        try {
            if (defaultMQProducer != null) {
                defaultMQProducer.start();
            }
        } catch (MQClientException e) {
            log.error("Failed to start the created producer instance.", e);
        }
    }

    public void close() {
        if (defaultMQProducer != null) {
            defaultMQProducer.shutdown();
        }
    }
}
