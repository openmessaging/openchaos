package io.openmessaging.driver.rabbitmq.utils;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class SingleConnectionFactory {
    private static volatile Map<ConnectionFactory, Connection> connections = new HashMap<>();

    private SingleConnectionFactory(){}

    public static Connection getConnection(ConnectionFactory factory) throws IOException, TimeoutException {
        Connection connection = connections.getOrDefault(factory, null);
        if (connection == null){
            synchronized (SingleConnectionFactory.class){
                if (connection == null ){
                    connection = factory.newConnection();
                    connections.put(factory, connection);
                }
            }
        }
        return connection;
    }

    public static void handleCloseExecption(ConnectionFactory factory) throws IOException, TimeoutException {
        synchronized (SingleConnectionFactory.class){
            Connection connection = factory.newConnection();
            connections.put(factory, connection);
        }
    }
}
