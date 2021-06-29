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

package io.openchaos.driver.redis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.kv.KVClient;
import io.openchaos.driver.kv.KVDriver;
import io.openchaos.driver.redis.config.RedisClientConfig;
import io.openchaos.driver.redis.config.RedisConfig;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class RedisDriver implements KVDriver {

    private List<String> nodes;
    private List<String> metaNodes;
    private RedisConfig redisConfig;
    private RedisClientConfig redisClientConfig;
    private int port;
    private String host;
    private String masterId = "mymaster";
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static RedisConfig readConfigForRedis(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RedisConfig.class);
    }

    private static RedisClientConfig readConfigForNode(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RedisClientConfig.class);
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.redisConfig = readConfigForRedis(configurationFile);
        this.redisClientConfig = readConfigForNode(configurationFile);
        this.nodes = nodes;
        this.host = redisClientConfig.host;
        this.port = redisClientConfig.port;
        if (redisClientConfig.masterId != null && !redisClientConfig.masterId.isEmpty()) {
            this.masterId = redisClientConfig.masterId;
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public ChaosNode createChaosNode(String node, List<String> nodes) {
        this.nodes = nodes;
        return new RedisNode(node, nodes, redisConfig);
    }

    @Override
    public MetaNode createPreChaosNode(String node, List<String> nodes)  {
        metaNodes = nodes;
        return new RedisSentinelNode(node, nodes, redisConfig);
    }
    
    @Override
    public String getStateName() {
        return "io.openchaos.driver.redis.RedisState";
    }
    
    @Override
    public KVClient createClient() {
        RedisURI sentinelUri = RedisURI.Builder.sentinel(host, port, masterId).build();
        //sentinelUri.setPassword("*******".toCharArray());
        sentinelUri.setTimeout(Duration.ofSeconds(5000));
        sentinelUri.setDatabase(0);
        RedisClient redisClient = RedisClient.create();
        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(redisClient, StringCodec.UTF8, sentinelUri);
        connection.setReadFrom(ReadFrom.REPLICA);
        return new RedisChaosClient(redisClient, connection);
    }

    @Override
    public String getMetaNode() {
        return host + ":" + port;
    }

    @Override
    public String getMetaName() {
        return masterId;
    }


}
