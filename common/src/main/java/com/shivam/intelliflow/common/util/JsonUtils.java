package com.shivam.intelliflow.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize value to JSON", exception);
        }
    }

    public static <T> T fromJson(String json, Class<T> targetType) {
        try {
            return OBJECT_MAPPER.readValue(json, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON", exception);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> targetType) {
        try {
            return OBJECT_MAPPER.readValue(json, targetType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to deserialize JSON", exception);
        }
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER.copy();
    }
}
