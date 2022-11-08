package io.openchaos.driver.elasticsearch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.elasticsearch.config.ElasticSearchClientConfig;
import io.openchaos.driver.elasticsearch.config.ElasticSearchConfig;
import io.openchaos.driver.elasticsearch.core.ElasticSearchFactory;
import io.openchaos.driver.kv.KVClient;
import io.openchaos.driver.kv.KVDriver;
import org.elasticsearch.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ElasticSearchDriver implements KVDriver {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchDriver.class);
    private List<String> nodes;
    private List<String> metaNodes;
    private ElasticSearchConfig elasticsearchConfig;
    private ElasticSearchClientConfig elasticsearchClientConfig;
    private int port;
    private String host;
    private String username;
    private String password;
    private final String endpoint = "/openchaos";

    @Override
    public String getMetaNode() {
        return null;
    }

    @Override
    public String getMetaName() {
        return null;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static ElasticSearchConfig readConfigForES(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, ElasticSearchConfig.class);
    }

    private static ElasticSearchClientConfig readConfigForNode(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, ElasticSearchClientConfig.class);
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.elasticsearchConfig = readConfigForES(configurationFile);
        this.elasticsearchClientConfig = readConfigForNode(configurationFile);
        this.nodes = nodes;
        this.host = elasticsearchClientConfig.host;
        this.username = elasticsearchClientConfig.username;
        this.password = elasticsearchClientConfig.password;
        this.port = elasticsearchClientConfig.port;
        ElasticSearchFactory.initial(this.nodes, username, password, elasticsearchConfig.isSsl, port);
        try {
            Request deleteRequest = new Request("DELETE", endpoint);
            ElasticSearchFactory.getClient().performRequest(deleteRequest);
            Request request = new Request("PUT", endpoint);
            ElasticSearchFactory.getClient().performRequest(request);
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        try {
            ElasticSearchFactory.close();
        } catch (Exception e) {
            log.error("Close es client failed!");
        }

    }

    @Override
    public ChaosNode createChaosNode(String node, List<String> nodes) {
        ElasticSearchChaosNode chaosNode = new ElasticSearchChaosNode(node, nodes, elasticsearchConfig);
        return chaosNode;
    }

    @Override
    public String getStateName() {
        return "io.openchaos.driver.elasticsearch.ElasticSearchState";
    }

    @Override
    public KVClient createClient() {
        ElasticSearchChaosClient client = null;
        try {
            client = new ElasticSearchChaosClient(ElasticSearchFactory.getClient());
        } catch (Exception e) {
            log.error("Create ES client failed!");
        }
        return client;
    }

    public ElasticSearchConfig getElasticsearchConfig() {
        return elasticsearchConfig;
    }

    public void setElasticsearchConfig(ElasticSearchConfig elasticsearchConfig) {
        this.elasticsearchConfig = elasticsearchConfig;
    }

    public ElasticSearchClientConfig getElasticsearchClientConfig() {
        return elasticsearchClientConfig;
    }

    public void setElasticsearchClientConfig(ElasticSearchClientConfig elasticsearchClientConfig) {
        this.elasticsearchClientConfig = elasticsearchClientConfig;
    }
}
