package io.openmessaging.driver.rabbitmq;

import io.openchaos.driver.queue.QueueState;

import java.util.HashSet;
import java.util.Set;

public class RabbitMQChaosState implements QueueState {
    private String clusterName;
    @Override
    public void initialize(String metaName, String metaNode) {
        this.clusterName = metaName;
    }

    @Override
    public Set<String> getLeader() {
        Set<String> leaderAddrPort = new HashSet<>();

        // todo
        return leaderAddrPort;
    }

    @Override
    public void close() {
        // todo
    }
}
