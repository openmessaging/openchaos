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

import io.openchaos.driver.queue.QueueState;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;

import java.util.HashSet;
import java.util.Set;

public class RocketMQChaosState implements QueueState {

    private DefaultMQAdminExt rmqAdmin;
    private String clusterName;

    @Override
    public void initialize(String metaName, String metaNode) {
        this.clusterName = metaName;
        this.rmqAdmin = new DefaultMQAdminExt();
        this.rmqAdmin.setNamesrvAddr(metaNode);
        try {
            this.rmqAdmin.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getLeader() {
        Set<String> leaderAddrPort = new HashSet<>();

        try {
            leaderAddrPort = CommandUtil.fetchMasterAddrByClusterName(rmqAdmin, clusterName);
        } catch (InterruptedException | RemotingTimeoutException | RemotingSendRequestException | MQBrokerException | RemotingConnectException e) {
            e.printStackTrace();
        }

        return leaderAddrPort;
    }

    @Override
    public void close() {
        if (rmqAdmin != null) {
            rmqAdmin.shutdown();
        }

    }
}
