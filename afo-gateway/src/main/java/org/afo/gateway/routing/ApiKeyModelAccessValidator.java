package org.afo.gateway.routing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API Key 模型授权验证器。
 *
 * <p>keyScope 使用模型编码列表语义，支持逗号分隔、JSON 字符串数组，以及 "*" 通配全部模型。</p>
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Component
public class ApiKeyModelAccessValidator {

    private static final String API_KEY_MODEL_SCOPE_EMPTY = "API_KEY_MODEL_SCOPE_EMPTY";
    private static final String API_KEY_MODEL_SCOPE_INVALID = "API_KEY_MODEL_SCOPE_INVALID";
    private static final String API_KEY_MODEL_NOT_ALLOWED = "API_KEY_MODEL_NOT_ALLOWED";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ModelAccessDecision evaluate(String keyScope, String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return ModelAccessDecision.allowed();
        }
        ParseResult parseResult = parseAuthorizedModels(keyScope);
        if (!parseResult.valid()) {
            return ModelAccessDecision.denied(API_KEY_MODEL_SCOPE_INVALID, null);
        }
        Set<String> authorizedModels = parseResult.models();
        if (authorizedModels.isEmpty()) {
            return ModelAccessDecision.denied(API_KEY_MODEL_SCOPE_EMPTY, null);
        }
        if (authorizedModels.contains("*") || authorizedModels.contains(modelCode)) {
            return ModelAccessDecision.allowed();
        }
        return ModelAccessDecision.denied(API_KEY_MODEL_NOT_ALLOWED, null);
    }

    private ParseResult parseAuthorizedModels(String keyScope) {
        if (keyScope == null || keyScope.isBlank()) {
            return ParseResult.valid(Set.of());
        }
        String trimmed = keyScope.trim();
        if (trimmed.startsWith("[") || trimmed.endsWith("]")) {
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                return ParseResult.invalid();
            }
            try {
                return ParseResult.valid(OBJECT_MAPPER.readValue(trimmed, new TypeReference<Set<String>>() {}).stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toSet()));
            } catch (Exception ignored) {
                return ParseResult.invalid();
            }
        }
        return ParseResult.valid(Arrays.stream(trimmed.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toSet()));
    }

    private record ParseResult(boolean valid, Set<String> models) {

        private static ParseResult valid(Set<String> models) {
            return new ParseResult(true, models);
        }

        private static ParseResult invalid() {
            return new ParseResult(false, Set.of());
        }
    }
}
