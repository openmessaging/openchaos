/*
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
package io.openchaos.common.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceUtil {
    private static final String STOP_ACTION = "stop";
    private static final String START_ACTION = "start";
    private static final String RESTART_ACTION = "restart";

    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

    public static void stop(String node, String serviceName) throws Exception {
        log.info("Start stop node {} service {}.", node, serviceName);
        action(node, serviceName, STOP_ACTION);
    }

    public static void start(String node, String serviceName) throws Exception {
        log.info("Start start node {} service {}.", node, serviceName);
        action(node, serviceName, START_ACTION);
    }

    public static void restart(String node, String serviceName) throws Exception {
        log.info("Start restart node {} service {}.", node, serviceName);
        action(node, serviceName, RESTART_ACTION);
    }

    private static void action(String node, String serviceName, String action) throws Exception {
        SshUtil.execCommand(node, String.format("service %s %s", serviceName, action));
    }
}