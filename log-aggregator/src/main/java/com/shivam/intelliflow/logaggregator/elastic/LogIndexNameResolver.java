package com.shivam.intelliflow.logaggregator.elastic;

import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogIndexNameResolver {
    private static final DateTimeFormatter DAILY_INDEX_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC);

    private final String indexPrefix;

    public LogIndexNameResolver(
            @Value("${intelliflow.log-aggregator.elasticsearch.index-prefix:intelliflow-logs}") String indexPrefix
    ) {
        this.indexPrefix = indexPrefix;
    }

    public String indexName(LogEventEnvelope envelope) {
        Instant indexInstant = envelope.event().timestamp() == null
                ? envelope.receivedAt()
                : envelope.event().timestamp();
        return indexPrefix + "-" + DAILY_INDEX_DATE_FORMATTER.format(indexInstant);
    }

    public String indexPattern() {
        return indexPrefix + "-*";
    }
}
