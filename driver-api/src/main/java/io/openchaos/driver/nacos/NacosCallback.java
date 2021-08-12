package io.openchaos.driver.nacos;

import io.openchaos.common.NacosMessage;

public interface NacosCallback {
    void messageReceived(NacosMessage message);
}
