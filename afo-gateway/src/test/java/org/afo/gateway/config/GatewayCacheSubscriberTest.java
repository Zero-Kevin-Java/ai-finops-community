package org.afo.gateway.config;

import org.afo.gateway.routing.ModelAccessMatcher;
import org.afo.gateway.routing.DefaultRoutingConfigClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class GatewayCacheSubscriberTest {

    private final ReactiveRedisTemplate<String, String> redisTemplate = mock(ReactiveRedisTemplate.class);
    private final ModelAccessMatcher modelAccessMatcher = mock(ModelAccessMatcher.class);
    private final DefaultRoutingConfigClient routingConfigClient = mock(DefaultRoutingConfigClient.class);
    private final GatewayCacheSubscriber subscriber =
        new GatewayCacheSubscriber(redisTemplate, modelAccessMatcher, routingConfigClient);

    @Test
    void refreshesRoutingConfigCacheByTenant() {
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        subscriber.handleRefresh("routing-config:tenant-a");

        verify(redisTemplate).delete("gateway:routing-config:tenant-a");
        verify(routingConfigClient).refresh("tenant-a");
    }
}
