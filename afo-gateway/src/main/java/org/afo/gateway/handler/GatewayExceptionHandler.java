package org.afo.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器
 * 
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Order(-1)
@Component
public class GatewayExceptionHandler implements org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("[Gateway] Unhandled exception: {}", ex.getMessage(), ex);
        
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        
        HttpStatusCode status = resolveStatus(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String reason = status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : "Error";
        
        String body = String.format(
            "{\"code\":%d,\"msg\":\"%s\"}",
            status.value(),
            reason
        );
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        
        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)));
    }
    
    private HttpStatusCode resolveStatus(Throwable ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return errorResponse.getStatusCode();
        }
        if (ex instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;  // 400
        }
        if (ex instanceof java.util.concurrent.TimeoutException ||
            ex instanceof java.io.IOException) {
            return HttpStatus.GATEWAY_TIMEOUT;  // 504
        }
        if (ex instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
            return HttpStatus.BAD_GATEWAY;  // 502
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;  // 500
    }

}
