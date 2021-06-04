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

package io.openchaos.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KillProcessUtil {

    private static final Logger log = LoggerFactory.getLogger(KillProcessUtil.class);

    public static void kill(String node, String processName) throws Exception {
        log.info("Kill node {} process {} .", node, processName);
        String pidList = SshUtil.execCommandWithArgsReturnStr(node, String.format("ps ax | grep -i '%s' | grep java | grep -v grep | awk '{print $1}'", processName)).trim();
        if (!pidList.isEmpty()) {
            String[] pids = pidList.split("\n");
            for (String pid : pids) {
                SshUtil.execCommand(node, String.format("kill %s", pid));
            }
        } else {
            log.info("No {} process running in node {}.", processName, node);
        }
    }

    public static void forceKill(String node, String processName) throws Exception {
        log.info("Force kill node {} process {} .", node, processName);
        String pidList = SshUtil.execCommandWithArgsReturnStr(node, String.format("ps ax | grep -i '%s' | grep java | grep -v grep | awk '{print $1}'", processName)).trim();
        if (!pidList.isEmpty()) {
            String[] pids = pidList.split("\n");
            for (String pid : pids) {
                SshUtil.execCommand(node, String.format("kill -9 %s", pid));
            }
        } else {
            log.info("No {} process running in node {}.", processName, node);
        }
    }
}
