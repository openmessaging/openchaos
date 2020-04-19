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

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshUtil {

    private static final int PORT = 22;
    private static final Logger log = LoggerFactory.getLogger(SshUtil.class);
    private static SshClient client;
    private static Map<String, ClientSession> sessionMap = new HashMap<>();
    private static Set<String> nodeSet = new HashSet<>();
    private static String username;

    public static void init(String username, List<String> nodes) throws Exception {
        SshUtil.username = username;
        client = SshClient.setUpDefaultClient();
        client.start();
        if (nodes != null) {
            nodeSet.addAll(nodes);
        }
    }

    public static void execCommand(String node, String cmd) throws Exception {
        ClientSession session = getSession(node);
        if (session != null) {
            log.debug("Exec command : {}", cmd);
            session.executeRemoteCommand(cmd, System.out, System.err, Charset.defaultCharset());
        } else {
            throw new RuntimeException("node is not in current config file, ssh execCommand command failed");
        }
    }

    public static void execCommandWithArgs(String node, String cmd, String... args) throws Exception {
        ClientSession session = getSession(node);
        if (session != null) {
            StringBuilder builder = new StringBuilder(cmd);
            for (int i = 0; i < args.length; i++) {
                builder.append(" ").append(args[i]);
            }
            cmd = builder.toString();
            log.debug("Exec command : {}", cmd);
            session.executeRemoteCommand(cmd, System.out, System.err, Charset.defaultCharset());
        } else {
            throw new RuntimeException("node is not in current config file, ssh execCommand command failed");
        }
    }

    public static String execCommandWithArgsReturnStr(String node, String cmd, String... args) throws Exception {
        ClientSession session = getSession(node);
        if (session != null) {
            StringBuilder builder = new StringBuilder(cmd);
            for (int i = 0; i < args.length; i++) {
                builder.append(" ").append(args[i]);
            }
            cmd = builder.toString();
            log.debug("Exec command : {}", cmd);
            return session.executeRemoteCommand(cmd);
        } else {
            throw new RuntimeException("Node is not in current config file, ssh execCommand command failed");
        }
    }

    public static void execCommandInDir(String node, String dir, String... cmd) throws Exception {
        ClientSession session = getSession(node);
        StringBuilder command = new StringBuilder("cd " + dir);
        Arrays.stream(cmd).forEach(x -> command.append(";").append(x));
        if (session != null) {
            log.debug("Exec command : {}", command.toString());
            session.executeRemoteCommand(command.toString(), System.out, System.err, Charset.defaultCharset());
        } else {
            throw new RuntimeException("Node is not in current config file, ssh execCommand command failed");
        }
    }

    public static void close() {
        sessionMap.entrySet().stream().map(Map.Entry::getValue).forEach(session -> {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Config session close failed", e);
            }
        });
        sessionMap.clear();
        if (client != null) {
            client.stop();
        }
    }

    private static synchronized ClientSession getSession(String node) throws Exception {
        if (!nodeSet.contains(node))
            return null;
        ClientSession session = sessionMap.get(node);
        if (session == null || !session.isOpen()) {
            if (Utils.isIp(node)) {
                session = client.connect(username, new InetSocketAddress(node, 22)).verify().getSession();
            } else {
                session = client.connect(username, node, PORT).verify().getSession();
            }
            session.auth().verify();
            sessionMap.put(node, session);
        }
        session.resetIdleTimeout();
        return session;
    }

}
