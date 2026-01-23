package org.example.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public final class JsonUtils {

    private final JsonMapper MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false) // ISO 8601 문자열
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // JSON에 없는 필드가 있어도 무시하고 진행
        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
        .build();

    public String toJson(Object obj) {
        try {
            return toJsonThrowable(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public String toJsonThrowable(Object obj) throws JsonProcessingException {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패: {}", e.getMessage());
            throw e;
        }
    }

    public <T> T toObject(String json, Class<T> clazz) {
        try {
            return toObjectThrowable(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public <T> T toObjectThrowable(String json, Class<T> clazz) throws JsonProcessingException {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 역직렬화 실패: {}", e.getMessage());
            throw e;
        }
    }
}
