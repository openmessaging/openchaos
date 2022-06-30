package io.openmessaging.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import io.openmessaging.driver.rabbitmq.utils.SingleConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DefaultRabbitMQProducer {
    private static final Logger log = LoggerFactory.getLogger(DefaultRabbitMQProducer.class);
    private ConnectionFactory factory;
    private String host = "127.0.0.1";
    private int port = 5672;
    private String user = "guest";
    private String password = "guest";

    public DefaultRabbitMQProducer(){}

    public  DefaultRabbitMQProducer(String host, int port, String user, String password){
        if (notNull(host)){
            this.host = host;
        }
        if (port != -1){
            this.port = port;
        }
        if (notNull(user)){
            this.user = user;
        }
        if (notNull(password)){
            this.password = password;
        }
    }

    public void init(){
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
    }

    private  boolean notNull(String s){
        return s != null && !s.equals("");
    }

    public void sendMessage(String queueName, byte[] message) throws IOException, TimeoutException {
        String var1 = "";
        Connection connection = SingleConnectionFactory.getConnection(factory);
        try(Channel channel = connection.createChannel())
        {
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicPublish(var1, queueName, null, message);
        }catch (ShutdownSignalException sse) {
            // possibly check if channel was closed
            // by the time we started action and reasons for
            // closing it
            log.warn("connection or channel is shutdown");
            SingleConnectionFactory.handleCloseExecption(factory);
        } catch (IOException ioe) {
            // check why connection was closed
            log.warn("IO was blocked");
        }
    }


}
