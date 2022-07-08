package io.openmessaging.driver.rabbitmq.config;

public class RabbitMQBrokerConfig {
    public String installDir;
    public String RabbitMQVersion;
    public int port;
    public String haPolicy;  // mirror or quorum
    public String haParams;
}
