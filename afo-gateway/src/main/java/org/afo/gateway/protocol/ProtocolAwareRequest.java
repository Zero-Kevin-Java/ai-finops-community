package org.afo.gateway.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class ProtocolAwareRequest {

    private final String protocol;
    private final JsonNode rawBody;
    private final String model;
    private final boolean stream;
    private final String promptText;
    private final String canonicalHashSource;
    private final List<String> toolNames;
    private final String systemPrompt;
    private final Integer maxTokens;
    private final BigDecimal temperature;
    private final byte[] rawBodyBytes;

    public static ProtocolAwareRequest from(String path, byte[] bodyBytes, ObjectMapper mapper) {
        String protocol = detectProtocol(path);
        if (bodyBytes == null || bodyBytes.length == 0) {
            return empty(protocol);
        }
        try {
            JsonNode node = mapper.readTree(bodyBytes);
            if ("anthropic".equals(protocol)) {
                return parseAnthropic(node, bodyBytes, mapper);
            } else {
                return parseOpenAI(node, bodyBytes);
            }
        } catch (Exception e) {
            return empty(protocol);
        }
    }

    public static String detectProtocol(String path) {
        if (path != null && (path.startsWith("/v1/messages") || path.startsWith("/messages"))) {
            return "anthropic";
        }
        return "openai";
    }

    private static ProtocolAwareRequest parseOpenAI(JsonNode node, byte[] bodyBytes) {
        String model = node.has("model") ? node.get("model").asText(null) : null;
        boolean stream = node.has("stream") && node.get("stream").asBoolean(false);
        String promptText = extractOpenAIPromptText(node);
        String canonicalHash = buildOpenAICanonicalHash(node);
        List<String> toolNames = extractOpenAIToolNames(node);
        String systemPrompt = extractOpenAISystemPrompt(node);
        Integer maxTokens = node.has("max_tokens") ? node.get("max_tokens").asInt() : null;
        BigDecimal temperature = node.has("temperature")
            ? node.get("temperature").decimalValue() : null;

        return ProtocolAwareRequest.builder()
            .protocol("openai")
            .rawBody(node)
            .model(model)
            .stream(stream)
            .promptText(promptText)
            .canonicalHashSource(canonicalHash)
            .toolNames(toolNames)
            .systemPrompt(systemPrompt)
            .maxTokens(maxTokens)
            .temperature(temperature)
            .rawBodyBytes(bodyBytes)
            .build();
    }

    private static ProtocolAwareRequest parseAnthropic(JsonNode node, byte[] bodyBytes, ObjectMapper mapper) {
        String model = node.has("model") ? node.get("model").asText(null) : null;
        boolean stream = node.has("stream") && node.get("stream").asBoolean(false);
        String promptText = extractAnthropicPromptText(node);
        String canonicalHash = buildAnthropicCanonicalHash(node);
        List<String> toolNames = extractAnthropicToolNames(node);
        String systemPrompt = extractAnthropicSystemPrompt(node);
        Integer maxTokens = node.has("max_tokens") ? node.get("max_tokens").asInt() : null;
        BigDecimal temperature = node.has("temperature")
            ? node.get("temperature").decimalValue() : null;

        return ProtocolAwareRequest.builder()
            .protocol("anthropic")
            .rawBody(node)
            .model(model)
            .stream(stream)
            .promptText(promptText)
            .canonicalHashSource(canonicalHash)
            .toolNames(toolNames)
            .systemPrompt(systemPrompt)
            .maxTokens(maxTokens)
            .temperature(temperature)
            .rawBodyBytes(bodyBytes)
            .build();
    }

    private static String extractOpenAIPromptText(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        JsonNode messages = node.path("messages");
        if (messages.isArray()) {
            for (JsonNode msg : messages) {
                JsonNode content = msg.path("content");
                if (content.isTextual() && sb.length() < 512) {
                    sb.append(content.asText()).append(" ");
                }
            }
        }
        if (sb.length() == 0 && node.has("prompt")) {
            String p = node.get("prompt").asText("");
            sb.append(p.length() > 512 ? p.substring(0, 512) : p);
        }
        return sb.toString().trim();
    }

    private static String extractAnthropicPromptText(JsonNode node) {
        StringBuilder sb = new StringBuilder();

        JsonNode system = node.path("system");
        if (system.isTextual() && sb.length() < 512) {
            sb.append(system.asText()).append(" ");
        } else if (system.isArray()) {
            for (JsonNode block : system) {
                if ("text".equals(block.path("type").asText(""))
                    && block.has("text") && sb.length() < 512) {
                    sb.append(block.path("text").asText()).append(" ");
                }
            }
        }

        JsonNode messages = node.path("messages");
        if (messages.isArray()) {
            for (JsonNode msg : messages) {
                JsonNode content = msg.path("content");
                if (content.isTextual() && sb.length() < 512) {
                    sb.append(content.asText()).append(" ");
                } else if (content.isArray()) {
                    for (JsonNode block : content) {
                        if ("text".equals(block.path("type").asText(""))
                            && block.has("text") && sb.length() < 512) {
                            sb.append(block.path("text").asText()).append(" ");
                        }
                    }
                }
            }
        }
        return sb.toString().trim();
    }

    private static String buildOpenAICanonicalHash(JsonNode node) {
        JsonNode messages = node.path("messages");
        if (!messages.isArray()) {
            return "";
        }
        List<String> roleOrder = List.of("system", "user", "assistant", "tool");
        List<String> parts = new ArrayList<>();
        for (JsonNode msg : messages) {
            if (msg.has("role") && msg.has("content")) {
                String role = msg.get("role").asText("");
                String text = extractTextContent(msg.get("content"));
                parts.add(role + ":" + text);
            }
        }
        parts.sort((a, b) -> {
            int ia = roleOrder.indexOf(a.split(":")[0]);
            int ib = roleOrder.indexOf(b.split(":")[0]);
            int va = ia >= 0 ? ia : 99;
            int vb = ib >= 0 ? ib : 99;
            return Integer.compare(va, vb);
        });
        return String.join("\n", parts).trim();
    }

    private static String buildAnthropicCanonicalHash(JsonNode node) {
        List<String> parts = new ArrayList<>();
        JsonNode system = node.path("system");
        if (system.isTextual()) {
            parts.add("system:" + system.asText().trim());
        } else if (system.isArray()) {
            for (JsonNode block : system) {
                if ("text".equals(block.path("type").asText("")) && block.has("text")) {
                    parts.add("system:" + block.path("text").asText().trim());
                }
            }
        }
        JsonNode messages = node.path("messages");
        if (messages.isArray()) {
            for (JsonNode msg : messages) {
                String role = msg.path("role").asText("");
                JsonNode content = msg.path("content");
                String text = extractTextContent(content);
                parts.add(role + ":" + text);
            }
        }
        return String.join("\n", parts).trim();
    }

    private static String extractTextContent(JsonNode content) {
        if (content.isTextual()) {
            return content.asText().trim();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText(""))) {
                    sb.append(item.path("text").asText(""));
                }
            }
            return sb.toString().trim();
        }
        return "";
    }

    private static List<String> extractOpenAIToolNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        JsonNode tools = node.path("tools");
        if (tools.isArray()) {
            for (JsonNode tool : tools) {
                String name = tool.at("/function/name").asText(null);
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        JsonNode functions = node.path("functions");
        if (functions.isArray()) {
            for (JsonNode fn : functions) {
                String name = fn.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private static List<String> extractAnthropicToolNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        JsonNode tools = node.path("tools");
        if (tools.isArray()) {
            for (JsonNode tool : tools) {
                String name = tool.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private static String extractOpenAISystemPrompt(JsonNode node) {
        JsonNode messages = node.path("messages");
        if (messages.isArray() && messages.size() > 0) {
            JsonNode first = messages.get(0);
            if ("system".equals(first.path("role").asText(""))) {
                JsonNode content = first.path("content");
                if (content.isTextual()) {
                    return content.asText();
                }
                if (content.isArray()) {
                    return extractTextContent(content);
                }
            }
        }
        return "";
    }

    private static String extractAnthropicSystemPrompt(JsonNode node) {
        JsonNode system = node.path("system");
        if (system.isTextual()) {
            return system.asText();
        }
        if (system.isArray()) {
            return extractTextContent(system);
        }
        return "";
    }

    private static ProtocolAwareRequest empty(String protocol) {
        return ProtocolAwareRequest.builder()
            .protocol(protocol)
            .rawBody(null)
            .model(null)
            .stream(false)
            .promptText("")
            .canonicalHashSource("")
            .toolNames(Collections.emptyList())
            .systemPrompt("")
            .maxTokens(null)
            .temperature(null)
            .rawBodyBytes(new byte[0])
            .build();
    }

    public byte[] rewriteModel(String newModel, ObjectMapper mapper) {
        if (rawBody == null) {
            return rawBodyBytes;
        }
        try {
            ObjectNode copy = (ObjectNode) rawBody.deepCopy();
            copy.put("model", newModel);
            return mapper.writeValueAsBytes(copy);
        } catch (Exception e) {
            return rawBodyBytes;
        }
    }
}
