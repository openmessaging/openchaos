/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openchaos.driver.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.queue.*;
import io.openchaos.driver.rabbitmq.config.RabbitMQBrokerConfig;
import io.openchaos.driver.rabbitmq.config.RabbitMQClientConfig;
import io.openchaos.driver.rabbitmq.config.RabbitMQConfig;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import io.openchaos.driver.rabbitmq.core.Sync;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class RabbitMQDriver implements QueueDriver {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQDriver.class);
    private RabbitMQClientConfig rmqClientConfig;
    private RabbitMQBrokerConfig rmqBrokerConfig;
    private RabbitMQConfig rmqConfig;
    private List<String> nodes;
    private List<String> metaNodes;
    private String user = "guest";
    private String password = "guest";
    private String queueName = "openchaos_client_1";
    private ConnectionFactory factory;
    private RabbitMQChaosState state;
    private GenericObjectPool<Channel> stateChannelPool;
    private ObjectPool<Channel> producerChannelPool;
    private GenericObjectPool<Channel> consumerChannelPool;
    private Connection consumerConnection;
    private Connection producerConnection;
    private Connection stateConnection;
    private Sync sync;
    private volatile String curState = "stop";

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static RabbitMQClientConfig readConfigForClient(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RabbitMQClientConfig.class);
    }

    private static RabbitMQBrokerConfig readConfigForBroker(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RabbitMQBrokerConfig.class);
    }

    private static RabbitMQConfig readConfigForRMQ(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RabbitMQConfig.class);
    }


    @Override
    public String getMetaNode() {
        return null;
    }

    @Override
    public String getMetaName() {
        return null;
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.rmqClientConfig = readConfigForClient(configurationFile);
        this.rmqBrokerConfig = readConfigForBroker(configurationFile);
        this.rmqConfig = readConfigForRMQ(configurationFile);
        this.nodes = nodes;
        this.user = rmqClientConfig.user;
        this.password = rmqClientConfig.password;
        sync = new Sync(nodes);
    }

    public void intialState() {
        if (Objects.equals("finish", curState) || Objects.equals("start", curState)) {
            return;
        }
        curState = "start";
        state = new RabbitMQChaosState("", queueName, nodes.get(0), rmqBrokerConfig.haMode, user, password);
        ConnectionFactory tmpFac = new ConnectionFactory();
        tmpFac.setHost(nodes.get(0));
        tmpFac.setUsername(user);
        tmpFac.setPassword(password);
        try {
            Connection tmpCon = tmpFac.newConnection("tmp");
            Channel tmpChan = tmpCon.createChannel();
            tmpChan.queueDelete(queueName);
            tmpChan.queueDeclare(queueName, false, false, false, null);
            tmpChan.close();
            tmpCon.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        state = new RabbitMQChaosState("", queueName, state.getLeader().iterator().next(), rmqBrokerConfig.haMode, user, password);
        factory = new ConnectionFactory();
        factory.setHost(state.getLeader().iterator().next());
        factory.setUsername(user);
        factory.setPassword(password);
        try {
            GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(200);
            config.setMinIdle(4);
            config.setMaxIdle(8);
            config.setTestOnBorrow(true);
            stateConnection = factory.newConnection("openchaos_state");
            ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, stateConnection);
            stateChannelPool = new GenericObjectPool<Channel>(channelPoolFactory, config);
            producerConnection = factory.newConnection("openchaos_producer");
            consumerConnection = factory.newConnection("openchaos_consumer");
            ChannelPoolFactory producerChannelPoolFactory = new ChannelPoolFactory(factory, producerConnection);
            producerChannelPool = new GenericObjectPool<Channel>(producerChannelPoolFactory, config);
            ChannelPoolFactory consumerChannelPoolFactory = new ChannelPoolFactory(factory, consumerConnection);
            consumerChannelPool = new GenericObjectPool<Channel>(consumerChannelPoolFactory, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            curState = "finish";
        }
    }

    @Override
    public void shutdown() {
        try {
            if (stateChannelPool != null) {
                stateChannelPool.close();
            }
            if (producerChannelPool != null) {
                producerChannelPool.close();
            }
            if (consumerChannelPool != null) {
                consumerChannelPool.close();
            }
            if (stateConnection != null && stateConnection.isOpen()) {
                stateConnection.close();
            }
            if (producerConnection != null && producerConnection.isOpen()) {
                producerConnection.close();
            }
            if (consumerConnection != null && consumerConnection.isOpen()) {
                consumerConnection.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChaosNode createChaosNode(String node, List<String> nodes) {
        this.nodes = nodes;
        return new RabbitMQChaosNode(node, nodes, rmqConfig, rmqBrokerConfig, sync);
    }

    @Override
    public String getStateName() {
        return "io.openchaos.driver.rabbitmq.RabbitMQChaosState";
    }

    @Override
    public void createTopic(String topic, int partitions) {
    }

    @Override
    public QueueProducer createProducer(String topic) {
        if (Objects.equals("stop", curState)) {
            intialState();
        }
        return new RabbitMQChaosProducer(factory, queueName, producerConnection, producerChannelPool);
    }

    @Override
    public QueuePushConsumer createPushConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        if (Objects.equals("stop", curState)) {
            intialState();
        }
        RabbitMQChaosPushConsumer rabbitMQChaosPushConsumer;
        try {
            DefaultRabbitMQPushConsumer pushConsumer = new DefaultRabbitMQPushConsumer(factory, queueName, consumerCallback, subscriptionName, consumerChannelPool, consumerConnection);
            rabbitMQChaosPushConsumer = new RabbitMQChaosPushConsumer(pushConsumer,
                    factory, queueName, subscriptionName, consumerCallback, consumerChannelPool, consumerConnection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rabbitMQChaosPushConsumer;
    }

    @Override
    public QueuePullConsumer createPullConsumer(String topic, String subscriptionName) {
        if (Objects.equals("stop", curState)) {
            intialState();
        }
        return new RabbitMQChaosPullConsumer(factory, queueName, subscriptionName, consumerChannelPool, consumerConnection);
    }


}
