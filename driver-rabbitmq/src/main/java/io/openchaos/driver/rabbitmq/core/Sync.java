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
package io.openchaos.driver.rabbitmq.core;

import io.openchaos.common.utils.SshUtil;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Sync {
    private CountDownLatch countDownLatch;
    private CountDownLatch cookieLatch;
    private CountDownLatch joinLatch;
    private String leader;
    private final String cookie = "openchaoscookie";
    public String status = "wait";

    public Sync(List<String> nodes) {
        countDownLatch = new CountDownLatch(nodes.size());
        cookieLatch = new CountDownLatch(nodes.size());
        joinLatch = new CountDownLatch(nodes.size());
        if (nodes.size() != 0) leader = nodes.get(0);
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public CountDownLatch getCookieLatch() {
        return cookieLatch;
    }

    public String getLeader() {
        return leader;
    }

    public void syncCookie(String node) throws Exception {
        String cmd = "echo '" + cookie + "' > ~/.erlang.cookie";
        SshUtil.execCommand(node, cmd);
    }

    public CountDownLatch getJoinLatch() {
        return joinLatch;
    }

    public void addUser(String username, String password) {
        try {
            SshUtil.execCommand(leader, "rabbitmqctl add_user " + username + " " + password);
            SshUtil.execCommand(leader, "rabbitmqctl set_user_tags " + username + " administrator");
            SshUtil.execCommand(leader, "rabbitmqctl set_permissions -p / " + username + " \".*\" \".*\" \".*\"");
        } catch (Exception ignored) {
        }
    }
}
