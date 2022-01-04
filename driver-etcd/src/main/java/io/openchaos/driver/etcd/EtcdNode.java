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

import io.openchaos.common.utils.ServiceUtil;
import io.openchaos.driver.ChaosNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class EtcdNode implements ChaosNode {

    private static final String ETCD_PROCESS_NAME = "etcd";
    private String node;
    private List<String> nodes;

    public EtcdNode(String node, List<String> nodes) {
        this.node = node;
        this.nodes = nodes;
    }

    @Override
    public void setup() {

    }

    @Override
    public void teardown() {
        stop();
    }

    @Override
    public void start() {
        try {
            ServiceUtil.start(this.node, ETCD_PROCESS_NAME);
            log.info("start etcd({}) success", this.node);
        } catch (Exception e) {
            log.error("start etcd({}) failed. ", this.node, e);
        }
    }

    @Override
    public void stop() {
        try {
            ServiceUtil.stop(this.node, ETCD_PROCESS_NAME);
            log.info("stop etcd({}) success", this.node);
        } catch (Exception e) {
            log.error("stop etcd({}) failed. ", this.node, e);
        }
    }

    @Override
    public void kill() {
        try {
            ServiceUtil.stop(this.node, ETCD_PROCESS_NAME);
            log.info("kill etcd({}) success", this.node);
        } catch (Exception e) {
            log.error("kill etcd({}) failed. ", this.node, e);
        }
    }

    @Override
    public void pause() {
        stop();
    }

    @Override
    public void resume() {
        start();
    }
}
