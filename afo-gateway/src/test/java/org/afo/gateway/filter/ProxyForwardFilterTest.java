package org.afo.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.afo.gateway.util.ProxyPathUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ProxyForwardFilterTest {

    @Test
    void proxyPathCoversResponsesAndEmbeddingsAndMessagesInterfaces() {
        assertTrue(ProxyPathUtils.isProxyPath("/v1/responses"));
        assertTrue(ProxyPathUtils.isProxyPath("/responses"));
        assertTrue(ProxyPathUtils.isProxyPath("/v1/embeddings"));
        assertTrue(ProxyPathUtils.isProxyPath("/embeddings"));
        assertTrue(ProxyPathUtils.isProxyPath("/v1/messages"));
        assertTrue(ProxyPathUtils.isProxyPath("/messages"));
    }

    @Test
    void proxyPathSkipsNonModelInterfaces() {
        assertFalse(ProxyPathUtils.isProxyPath("/v1/models"));
    }

    @Test
    void streamRequestIncludesUsageOptionForUpstreamUsageStats() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String body = ProxyForwardFilter.ensureStreamUsageIncluded(
            objectMapper,
            "{\"model\":\"gpt-4o-mini\",\"stream\":true,\"messages\":[]}");

        JsonNode node = objectMapper.readTree(body);
        assertTrue(node.path("stream").asBoolean());
        assertTrue(node.path("stream_options").path("include_usage").asBoolean());
    }

    @Test
    void nonStreamRequestDoesNotAddStreamOptions() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String body = ProxyForwardFilter.ensureStreamUsageIncluded(
            objectMapper,
            "{\"model\":\"gpt-4o-mini\",\"stream\":false,\"messages\":[]}");

        JsonNode node = objectMapper.readTree(body);
        assertEquals(false, node.has("stream_options"));
    }
}
