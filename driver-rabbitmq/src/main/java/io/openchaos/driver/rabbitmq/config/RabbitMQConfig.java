package io.openchaos.driver.rabbitmq.config;

public class RabbitMQConfig {
    public String installDir;
    public String rabbitmqVersion;
    public String configureFilePath;

    public RabbitMQConfig setInstallDir(String dir){
        installDir = dir;
        return this;
    }
}
