package com.shivam.intelliflow.common.kafka;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.JsonUtils;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public final class EventSchemaKafkaSerdes {
    private EventSchemaKafkaSerdes() {
    }

    public static JsonSerializer<EventSchema> serializer() {
        JsonSerializer<EventSchema> serializer = new JsonSerializer<>(JsonUtils.objectMapper());
        serializer.setAddTypeInfo(false);
        return serializer;
    }

    public static JsonDeserializer<EventSchema> deserializer() {
        JsonDeserializer<EventSchema> deserializer = new JsonDeserializer<>(
                EventSchema.class,
                JsonUtils.objectMapper(),
                false
        );
        deserializer.addTrustedPackages(EventSchema.class.getPackageName());
        deserializer.ignoreTypeHeaders();
        return deserializer;
    }
}
