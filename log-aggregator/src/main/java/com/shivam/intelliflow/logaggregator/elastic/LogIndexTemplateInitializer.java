package com.shivam.intelliflow.logaggregator.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "intelliflow.log-aggregator.elasticsearch.template",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LogIndexTemplateInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LogIndexTemplateInitializer.class);

    private final ElasticsearchClient elasticsearchClient;
    private final LogIndexNameResolver indexNameResolver;
    private final String templateName;
    private final String numberOfShards;
    private final String numberOfReplicas;

    public LogIndexTemplateInitializer(
            ElasticsearchClient elasticsearchClient,
            LogIndexNameResolver indexNameResolver,
            @Value("${intelliflow.log-aggregator.elasticsearch.template.name:intelliflow-logs-template}") String templateName,
            @Value("${intelliflow.log-aggregator.elasticsearch.template.number-of-shards:1}") String numberOfShards,
            @Value("${intelliflow.log-aggregator.elasticsearch.template.number-of-replicas:0}") String numberOfReplicas
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexNameResolver = indexNameResolver;
        this.templateName = templateName;
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            elasticsearchClient.indices().putIndexTemplate(indexTemplateRequest());
            log.info("Ensured Elasticsearch index template={} pattern={}", templateName, indexNameResolver.indexPattern());
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not initialize Elasticsearch index template={}; log aggregation will continue with PostgreSQL persistence",
                    templateName,
                    exception
            );
        }
    }

    private PutIndexTemplateRequest indexTemplateRequest() {
        return new PutIndexTemplateRequest.Builder()
                .name(templateName)
                .indexPatterns(indexNameResolver.indexPattern())
                .template(template -> template
                        .settings(settings -> settings
                                .numberOfShards(numberOfShards)
                                .numberOfReplicas(numberOfReplicas)
                        )
                        .mappings(mappings -> mappings
                                .dynamic(DynamicMapping.False)
                                .properties("@timestamp", property -> property.date(date -> date))
                                .properties("receivedAt", property -> property.date(date -> date))
                                .properties("eventId", property -> property.keyword(keyword -> keyword))
                                .properties("serviceName", property -> property.keyword(keyword -> keyword))
                                .properties("level", property -> property.keyword(keyword -> keyword))
                                .properties("traceId", property -> property.keyword(keyword -> keyword))
                                .properties("spanId", property -> property.keyword(keyword -> keyword))
                                .properties("message", property -> property.text(text -> text))
                                .properties("metadata", property -> property.flattened(flattened -> flattened))
                                .properties("rawEvent", property -> property.object(object -> object.enabled(false)))
                        )
                )
                .build();
    }
}
