package io.openmessaging.driver.rabbitmq.config;

public class RabbitMQBrokerConfig {
    public String brokerName;
    public String haMode;  // all or exactly or nodes
    public String haParams;
}
