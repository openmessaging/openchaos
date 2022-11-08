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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.queue.QueueState;
import io.openchaos.driver.rabbitmq.core.HaMode;
import io.openchaos.driver.rabbitmq.core.HttpClientFactory;
import io.openchaos.driver.rabbitmq.core.LeaderStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RabbitMQChaosState implements QueueState {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosState.class);
    private HaMode haMode;
    private String leader;
    private String user;
    private String password;

    public RabbitMQChaosState(String metaName, String queueName, String node, HaMode haMode, String user, String password) {
        this.haMode = haMode;
        this.leader = node;
        this.user = user;
        this.password = password;
        HttpClientFactory.initial(user, password);
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
            String url = "http://" + leader + ":15672/api/queues/%2f/openchaos_client_1";
            try {
                String content = sendGet(url);
                ObjectMapper objectMapper = new ObjectMapper();
                leader = objectMapper.readValue(content, LeaderStatus.class).getNode();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        leaderAddr.add(getHost(leader));
        return leaderAddr;
    }

    @Override
    public void close() {
    }

    private String getHost(String nodeName) {
        return nodeName.split("@")[1];
    }

    private String sendGet(String url) {
        CloseableHttpClient httpClient = HttpClientFactory.createCloseableHttpClientWithBasicAuth(null);
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return leader;
    }
}
