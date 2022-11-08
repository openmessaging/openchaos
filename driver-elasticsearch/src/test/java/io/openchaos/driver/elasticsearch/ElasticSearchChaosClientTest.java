package io.openchaos.driver.elasticsearch;

import io.openchaos.driver.elasticsearch.core.Document;
import io.openchaos.driver.elasticsearch.core.ElasticSearchFactory;
import junit.framework.TestCase;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElasticSearchChaosClientTest extends TestCase {

    static RestClient client = Mockito.mock(RestClient.class);
    static Response response = Mockito.mock(Response.class);
    static Document document = Mockito.mock(Document.class);
    static ElasticSearchChaosClient esClient = Mockito.mock(ElasticSearchChaosClient.class);
    static StatusLine statusLine = Mockito.mock(StatusLine.class);
    static {
        try {
            esClient.setEsClient(client);
            Mockito.when(client.performRequest(Mockito.any())).thenReturn(response);
            Mockito.when(response.getStatusLine()).thenReturn(statusLine);
            Mockito.when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            Mockito.when(document.getValue()).thenReturn("1");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    public void testPut() {
        esClient.put(Optional.empty(), "");
    }

    public void testGetAll() {
        assertNotNull(esClient.getAll(Optional.empty(), 10));
    }

}