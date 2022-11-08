package io.openchaos.driver.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openchaos.driver.ChaosState;
import io.openchaos.driver.elasticsearch.core.ElasticSearchFactory;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ElasticSearchState implements ChaosState {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchState.class);
    ObjectMapper objectMapper;

    @Override
    public void initialize(String metaName, String metaNode) {
        objectMapper = new ObjectMapper();
    }

    @Override
    public Set<String> getLeader() {
        Set<String> res = new HashSet<>();
        RestClient client = ElasticSearchFactory.getClient();
        Request request = new Request("GET", "_cat/nodes?v");
        try {
            Response response = client.performRequest(request);
            String entity = EntityUtils.toString(response.getEntity());
            String ip = parseEntity(entity);
            if (ip == null) {
                log.error("Can not find leader host!");
                return null;
            }
            res.add(ip);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public void close() {}

    public String parseEntity(String entity) {
        String[] strs = entity.split(" ");
        int index = 0;
        boolean find = false;
        for (; index < strs.length; ++index) {
            if (strs[index].equals("*")) {
                find = true;
                break;
            }
        }
        if (!find) {
            return null;
        }
        return index >= 5 ? strs[index - 5] : null;

    }
}
