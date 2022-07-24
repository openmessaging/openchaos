package io.openchaos.driver.rabbitmq.config;

import io.openchaos.driver.rabbitmq.core.HaMode;
import io.openchaos.driver.rabbitmq.core.HaParams;

public class RabbitMQBrokerConfig {
    public HaMode haMode;
    public HaParams haParams;  //todo yaml 文件里的这个对象还读不到
}
