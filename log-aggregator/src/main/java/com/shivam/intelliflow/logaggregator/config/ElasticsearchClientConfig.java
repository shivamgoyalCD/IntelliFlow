package com.shivam.intelliflow.logaggregator.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.shivam.intelliflow.common.util.JsonUtils;
import java.util.Arrays;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ElasticsearchClientConfig {
    @Bean
    public RestClient elasticsearchRestClient(
            @Value("${intelliflow.log-aggregator.elasticsearch.uris:http://localhost:9200}") String uris
    ) {
        return RestClient.builder(httpHosts(uris)).build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient elasticsearchRestClient) {
        return new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper(JsonUtils.objectMapper()));
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }

    private HttpHost[] httpHosts(String uris) {
        return Arrays.stream(uris.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isBlank())
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
    }
}
