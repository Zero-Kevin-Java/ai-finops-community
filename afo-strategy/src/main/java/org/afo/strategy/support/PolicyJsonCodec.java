package org.afo.strategy.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import lombok.RequiredArgsConstructor;
import org.afo.common.core.exception.ServiceException;
import org.afo.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * JSON parsing rules shared by strategy policies.
 */
@Component
@RequiredArgsConstructor
public class PolicyJsonCodec {

    private final ObjectMapper objectMapper;

    private JavaType stringListType() {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
    }

    public List<String> parseRequiredStringList(String json, String errorMessage) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.<List<String>>readValue(json, stringListType()).stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        } catch (Exception e) {
            throw new ServiceException(errorMessage);
        }
    }

    public List<String> parseLenientStringList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.<List<String>>readValue(json, stringListType()).stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        Iterator<JsonNode> iterator = node.elements();
        Set<String> values = new HashSet<>();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (item.isTextual() && StringUtils.isNotBlank(item.asText())) {
                values.add(item.asText());
            }
        }
        return values.stream().toList();
    }

    public JsonNode parseRequiredObject(String json, String errorMessage) {
        if (StringUtils.isBlank(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new ServiceException(errorMessage);
            }
            return node;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(errorMessage);
        }
    }

    public JsonNode parseLenientObject(String json) {
        if (StringUtils.isBlank(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}
