package io.openmessaging.chaos.driver;

public interface ChaosNode {

    /**
     * Set up the distributed system program to be tested on this particular node. No need to start process
     */
    void setup();

    /**
     * Teardown the distributed system program to be tested on this particular node
     */
    void teardown();

    /**
     * Start specified processes on this particular node
     */
    void start();

    /**
     * Stop specified processes on this particular node
     */
    void stop();

    /**
     * Kill specified processes on this particular node
     */
    void kill();

    /**
     * Pause specified processes on this particular node
     */
    void pause();

    /**
     * Resume specified processes on this particular node
     */
    void resume();

}
