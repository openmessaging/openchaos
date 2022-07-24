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

    public RabbitMQChaosState(String metaName, String queueName, String node, HaMode haMode) {
        this.haMode = haMode;
        this.queueName = queueName;
        this.leader = node;
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
            String command = "curl -s -u root:root http://" + leader + ":15672/api/queues/%2f/openchaos_client_1 | python -m json.tool | grep node";
            try {
                String s = SshUtil.execCommandWithArgsReturnStr(leader, command);
                String[] split = s.split(":");
                int l = 0;
                while (split[1].charAt(l) != '"'){
                    ++l;
                }
                int r = split[1].length() - 1;
                while (split[1].charAt(r) != '"'){
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

    private String getHost(String nodeName){
        return nodeName.split("@")[1];
    }
}
