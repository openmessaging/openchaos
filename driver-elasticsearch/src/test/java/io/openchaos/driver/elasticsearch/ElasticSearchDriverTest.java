package io.openchaos.driver.elasticsearch;

import io.openchaos.driver.elasticsearch.config.ElasticSearchConfig;
import io.openchaos.driver.elasticsearch.core.ElasticSearchFactory;
import junit.framework.TestCase;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class ElasticSearchDriverTest extends TestCase {
    static ElasticSearchDriver driver = new ElasticSearchDriver();
    static List<String> nodes = new ArrayList<>();
    static {
        ElasticSearchFactory.initial(new ArrayList<String>() {{
            add("localhost");
        }}, "elastic", "elastic", false, 9200);
    }


    public void testShutdown() {
        driver.shutdown();
    }

    public void testCreateChaosNode() {
        ElasticSearchConfig config = Mockito.mock(ElasticSearchConfig.class);
        driver.setElasticsearchConfig(config);
        assertNotNull(driver.createChaosNode("", nodes));
    }

    public void testGetStateName() {
        assertNotNull(driver.getStateName());
    }

    public void testCreateClient() {
        assertNotNull(driver.createClient());
    }
}