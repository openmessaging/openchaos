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

  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  import java.net.URI;
  import java.util.HashMap;
  import java.util.HashSet;
  import java.util.Set;
  import java.util.concurrent.ExecutionException;

  import io.etcd.jetcd.ByteSequence;
  import io.etcd.jetcd.Client;
  import io.etcd.jetcd.Cluster;
  import io.etcd.jetcd.cluster.Member;
  import io.etcd.jetcd.cluster.MemberListResponse;
  import io.etcd.jetcd.maintenance.StatusResponse;
  import io.openchaos.driver.ChaosState;

  public class EtcdState implements ChaosState {
      private static final Logger log = LoggerFactory.getLogger(EtcdState.class);
      private Client client;
      private String[] metaNode;


      @Override
      public void initialize(String metaData, String metaNode) {
          String username = metaData.split(",")[0].split(":")[1];
          String password = metaData.split(",")[1].split(":")[1];
          this.metaNode = metaNode.split(",");
          this.client = Client.builder().endpoints(this.metaNode)
                  .user(ByteSequence.from(username.getBytes()))
                  .password(ByteSequence.from(password.getBytes()))
                  .build();

      }

      @Override
      public Set<String> getLeader() {
          Set<String> leaderAddr = new HashSet<>();
          try {
              StatusResponse statusResponse = client.getMaintenanceClient().statusMember(
                      URI.create(metaNode[1])).get();
              if (statusResponse != null) {
                  long leaderId = statusResponse.getLeader();
                  Cluster clusterClient = client.getClusterClient();
                  MemberListResponse memberListResponse = clusterClient.listMember().get();
                  for (Member member : memberListResponse.getMembers()) {
                      if (member.getId() == leaderId) {
                          for (URI uri : member.getPeerURIs()) {
                              leaderAddr.add(uri.toString().replace("http://",""));
                          }
                      }
                  }
              }
          } catch (InterruptedException | ExecutionException e) {
              e.printStackTrace();
              log.error("EtcdState, get leader node error!");
          }
          return leaderAddr;
      }

      @Override
      public void close() {
          if (client != null) {
              client.close();
          }
      }
  }