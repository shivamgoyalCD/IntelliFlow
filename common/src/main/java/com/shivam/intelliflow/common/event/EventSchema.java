package com.shivam.intelliflow.common.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EventSchema(
        String eventId,
        Instant timestamp,
        String serviceName,
        EventLevel level,
        String traceId,
        String spanId,
        String message,
        Map<String, Object> metadata
) {
    public EventSchema {
        metadata = metadata == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
