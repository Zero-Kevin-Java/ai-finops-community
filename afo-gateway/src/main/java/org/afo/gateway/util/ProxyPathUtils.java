package org.afo.gateway.util;

/**
 * 共享路径判断工具类。
 *
 * @author AI-FinOps Team
 * @since 2026-06-02
 */
public final class ProxyPathUtils {

    private ProxyPathUtils() {
    }

    public static boolean isProxyPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/v1/chat/completions")
            || path.startsWith("/chat/completions")
            || path.startsWith("/v1/completions")
            || path.startsWith("/completions")
            || path.startsWith("/v1/responses")
            || path.startsWith("/responses")
            || path.startsWith("/v1/embeddings")
            || path.startsWith("/embeddings")
            || path.startsWith("/v1/audio/transcriptions")
            || path.startsWith("/v1/messages")
            || path.startsWith("/messages");
    }
}
