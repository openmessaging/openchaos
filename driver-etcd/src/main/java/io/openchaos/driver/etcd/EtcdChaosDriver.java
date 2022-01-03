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
  package io.openchaos.driver.etcd;

  import com.fasterxml.jackson.databind.DeserializationFeature;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
  import io.etcd.jetcd.ByteSequence;
  import io.etcd.jetcd.Client;
  import io.openchaos.driver.ChaosNode;
  import io.openchaos.driver.etcd.config.EtcdClientConfig;
  import io.openchaos.driver.etcd.config.EtcdConfig;
  import io.openchaos.driver.kv.KVClient;
  import io.openchaos.driver.kv.KVDriver;
  import lombok.extern.slf4j.Slf4j;

  import java.io.File;
  import java.io.IOException;
  import java.util.List;


  @Slf4j
  public class EtcdChaosDriver implements KVDriver {
      private EtcdConfig etcdConfig;
      private EtcdClientConfig etcdClientConfig;
      private List<String> endpoints;

      private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      private EtcdConfig readConfigForEtcd(File configurationFile) throws IOException {
          return MAPPER.readValue(configurationFile, EtcdConfig.class);
      }

      private EtcdClientConfig readEtcdClientConfig(File configurationFile) throws IOException {
          return MAPPER.readValue(configurationFile, EtcdClientConfig.class);
      }

      @Override
      public KVClient createClient() {
          Client client = Client.builder().endpoints(etcdConfig.getEndpoints().split(","))
                  .user(ByteSequence.from(etcdClientConfig.getUsername().getBytes()))
                  .password(ByteSequence.from(etcdClientConfig.getPassword().getBytes()))
                  .build();
          return new EtcdChaosClient(client);
      }

      @Override
      public void initialize(File configurationFile, List<String> nodes) throws IOException {
          etcdConfig = this.readConfigForEtcd(configurationFile);
          etcdClientConfig = this.readEtcdClientConfig(configurationFile);
      }

      @Override
      public void shutdown() {

      }

      @Override
      public String getStateName() {
          return "io.openchaos.driver.etcd.EtcdState";
      }

      @Override
      public ChaosNode createChaosNode(String node, List<String> nodes) {
          return new EtcdNode(node, nodes);
      }

      @Override
      public String getMetaNode() {
          return etcdConfig.getEndpoints();
      }

      @Override
      public String getMetaName() {
          return "username:" + etcdClientConfig.getUsername() + ",password:" + etcdClientConfig.getPassword();
      }
  }