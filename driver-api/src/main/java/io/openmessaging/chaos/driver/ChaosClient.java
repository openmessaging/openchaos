package io.openmessaging.chaos.driver;

public interface ChaosClient {

    /**
     * Start the ChaosClient
     */
    void start();

    /**
     * Close the ChaosClient
     */
    void close();
}

