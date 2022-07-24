package io.openchaos.driver.rabbitmq;

import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.rabbitmq.core.HaMode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.*;

public class RabbitMQChaosStateTest {
    static RabbitMQChaosState state = new RabbitMQChaosState("", "openchaos_client_1", "tcloud", HaMode.classic);
    static {
        try {
            SshUtil.init("root", "Yigeyy00", new ArrayList<String>(){{add("tcloud");}});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void getLeader() {
        Set<String> leader = state.getLeader();
        assertNotNull(leader);
    }
}