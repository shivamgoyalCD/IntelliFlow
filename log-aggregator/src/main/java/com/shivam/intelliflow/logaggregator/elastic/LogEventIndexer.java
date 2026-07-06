package com.shivam.intelliflow.logaggregator.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.shivam.intelliflow.logaggregator.model.LogEventBatch;
import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogEventIndexer {
    private static final Logger log = LoggerFactory.getLogger(LogEventIndexer.class);

    private final ElasticsearchClient elasticsearchClient;
    private final LogIndexNameResolver indexNameResolver;

    public LogEventIndexer(
            ElasticsearchClient elasticsearchClient,
            LogIndexNameResolver indexNameResolver
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexNameResolver = indexNameResolver;
    }

    public void indexBatch(LogEventBatch batch) {
        for (LogEventEnvelope envelope : batch.events()) {
            index(envelope);
        }
    }

    private void index(LogEventEnvelope envelope) {
        try {
            LogEventDocument document = LogEventDocument.from(envelope);
            IndexResponse response = elasticsearchClient.index(request -> request
                    .index(indexNameResolver.indexName(envelope))
                    .id(document.eventId())
                    .document(document)
            );

            log.debug(
                    "Indexed log event eventId={} index={} result={}",
                    document.eventId(),
                    response.index(),
                    response.result()
            );
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Failed to index log event eventId={} into Elasticsearch; PostgreSQL persistence remains authoritative",
                    envelope.event().eventId(),
                    exception
            );
        }
    }
}
