/**
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

package io.openmessaging.chaos.common.utils;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.List;

public class NetUtil {

    private static final String DELAY_MEAN = "100ms";

    private static final String DELAY_VARIANCE = "50ms";

    private static final String LOSS_RATE = "0.8";

    public static void partition(String node, String targetNode) throws Exception {
        if (!Utils.isIp(targetNode)) {
            targetNode = InetAddress.getByName(targetNode).getHostAddress();
        }
        SshUtil.execCommandWithArgs(node, "iptables", "-A", "INPUT", "-s", targetNode, "-j", "DROP", "-w");
    }

    public static void healPartition(String node) throws Exception {
        try {
            SshUtil.execCommandWithArgs(node, "iptables", "-F", "-w");
        } catch (RemoteException e) {
            SshUtil.execCommandWithArgs(node, "iptables", "-X", "-w");
        }
    }

    public static void delay(String node) throws Exception {
        SshUtil.execCommandWithArgs(node, "tc", "qdisc", "add", "dev", "eth0", "root", "netem", "delay", DELAY_MEAN, DELAY_VARIANCE);
    }

    public static void healDelay(String node) throws Exception {
        String output = null;
        try {
            output = SshUtil.execCommandWithArgsReturnStr(node, "tc", "qdisc", "del", "dev", "eth0", "root", "netem", "delay", DELAY_MEAN, DELAY_VARIANCE);
        } catch (RemoteException e) {
            if (output != null && !output.contains("RTNETLINK answers: Invalid argument")) {
                throw e;
            }
        }
    }

    public static void loss(String node, List<String> targetNodes) throws Exception {
        for (String targetNode : targetNodes) {
            if (!Utils.isIp(targetNode)) {
                targetNode = InetAddress.getByName(targetNode).getHostAddress();
            }
            SshUtil.execCommandWithArgsReturnStr(node, "iptables", "-A", "INPUT", "-s", targetNode, "-m", "statistic", "--mode", "random", "--probability", LOSS_RATE, "-j", "DROP");
        }
    }

    public static void healLoss(String node) throws Exception {
        try {
            SshUtil.execCommandWithArgs(node, "iptables", "-F", "-w");
        } catch (RemoteException e) {
            SshUtil.execCommandWithArgs(node, "iptables", "-X", "-w");
        }
    }
}
