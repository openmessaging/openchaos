/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openchaos.driver.rabbitmq;

import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.queue.QueueState;
import io.openchaos.driver.rabbitmq.core.HaMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class RabbitMQChaosState implements QueueState {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosState.class);
    private String queueName = "openchaos_client_1";
    private HaMode haMode;
    private String leader;
    private String user;
    private String password;

    public RabbitMQChaosState(String metaName, String queueName, String node, HaMode haMode, String user, String password) {
        this.haMode = haMode;
        this.queueName = queueName;
        this.leader = node;
        this.user = user;
        this.password = password;
    }

    @Override
    public void initialize(String metaName, String metaNode) {
        leader = metaNode;
    }

    @Override
    public Set<String> getLeader() {
        Set<String> leaderAddr = new HashSet<>();
        if (haMode == HaMode.quorum) {
            try {
                String res = SshUtil.execCommandWithArgsReturnStr(leader, "rabbitmq-queues quorum_status \"openchaos_client_1\" | grep leader ");
                String[] s = res.split(" ");
                leaderAddr.add(getHost(s[1]));
            } catch (Exception e) {
                log.warn("Get leader failed!");
            }
        } else if (haMode == HaMode.classic) {
            String command = "curl -s -u " + user + ":" + password + " http://" + leader + ":15672/api/queues/%2f/openchaos_client_1 | python -m json.tool | grep node";
            try {
                String s = SshUtil.execCommandWithArgsReturnStr(leader, command);
                String[] split = s.split(":");
                int l = 0;
                while (split[1].charAt(l) != '"') {
                    ++l;
                }
                int r = split[1].length() - 1;
                while (split[1].charAt(r) != '"') {
                    --r;
                }
                leaderAddr.add(getHost(split[1].substring(l + 1, r)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return leaderAddr;
    }

    @Override
    public void close() {
    }

    private String getHost(String nodeName) {
        return nodeName.split("@")[1];
    }
}
