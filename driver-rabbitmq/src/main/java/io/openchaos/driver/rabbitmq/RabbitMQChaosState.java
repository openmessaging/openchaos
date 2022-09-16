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
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
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
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://" + leader + ":15672/api/queues/%2f/openchaos_client_1");
            ArrayList<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair("username", user));
            parameters.add(new BasicNameValuePair("password", password));
            CloseableHttpResponse response = null;
            try {
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters);
                httpPost.setEntity(formEntity);
                httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String content = EntityUtils.toString(response.getEntity(), "UTF-8");

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (response != null) {
                        response.close();
                    }
                    httpClient.close();
                } catch (IOException e) {
                    log.error("httpclient close faild!");
                }
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
