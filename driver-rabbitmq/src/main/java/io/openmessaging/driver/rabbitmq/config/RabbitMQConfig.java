package io.openmessaging.driver.rabbitmq.config;

import io.openmessaging.driver.rabbitmq.RabbitMQChaosNode;

public class RabbitMQConfig {
    public String installDir;
    public String rabbitmqVersion;
    public String configureFilePath;

    public RabbitMQConfig setInstallDir(String dir){
        installDir = dir;
        return this;
    }
}
