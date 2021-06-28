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

package io.openchaos.driver.redis;

import io.openchaos.common.utils.KillProcessUtil;
import io.openchaos.common.utils.PauseProcessUtil;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.redis.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RedisSentinelNode implements MetaNode {
    private static final String SENTINEL_PROCESS_NAME = "redis-sentinel";
    String node;
    List<String> nodes;
    private static final Logger log = LoggerFactory.getLogger(RedisSentinelNode.class);
    private String installDir = "redis-chaos-test";
    private String redisVersion = "5.0.5";


    public RedisSentinelNode(String node, List<String> nodes, RedisConfig redisConfig) {
        this.node = node;
        this.nodes = nodes;
        if (redisConfig.installDir != null && !redisConfig.installDir.isEmpty()) {
            this.installDir = redisConfig.installDir;
        }
        if (redisConfig.redisVersion != null && !redisConfig.redisVersion.isEmpty()) {
            this.redisVersion = redisConfig.redisVersion;
        }
    }

    @Override
    public void setup() {
        log.info("Node {} download redis...", node);
        try {
            SshUtil.execCommand(node, String.format("rm -rf %s; mkdir %s", installDir, installDir));
            SshUtil.execCommandInDir(node, installDir,
                    String.format("wget http://download.redis.io/releases/redis-%s.tar.gz ", redisVersion),
                    String.format("mv redis-%s.tar.gz redis.tar.gz", redisVersion),
                    "tar -xvf redis.tar.gz", "rm -f redis.tar.gz", "mv redis-*/* .", "rm -rf redis-*", "make", "make install");
            log.info("Node {} download redis success", node);

            //For docker test
            //Prepare Sentinel conf
        } catch (Exception e) {
            log.error("Node {} setup redis node failed", node, e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public void teardown() {
        stop();
    }

    @Override
    public void start() {
        try {
            log.info("Node {} start sentinel...", node);
            SshUtil.execCommandInDir(node, installDir, "redis-sentinel sentinel.conf");

        } catch (Exception e) {
            log.error("Node {} start redis PreNode failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            KillProcessUtil.kill(node, SENTINEL_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} stop redis processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void kill() {
        try {
            KillProcessUtil.forceKill(node, SENTINEL_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} kill redis processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void pause() {
        try {
            PauseProcessUtil.suspend(node, SENTINEL_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} pause redis processes failed", node, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resume() {
        try {
            PauseProcessUtil.resume(node, SENTINEL_PROCESS_NAME);
        } catch (Exception e) {
            log.error("Node {} resume redis processes failed", node, e);
            throw new RuntimeException(e);
        }
    }
}