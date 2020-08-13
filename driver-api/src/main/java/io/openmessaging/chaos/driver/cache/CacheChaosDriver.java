package io.openmessaging.chaos.driver.cache;

import io.openmessaging.chaos.driver.ChaosClient;
import io.openmessaging.chaos.driver.ChaosDriver;
import java.util.Optional;

public interface CacheChaosDriver extends ChaosDriver {
    CacheChaosClient createCacheChaosClient();
}
