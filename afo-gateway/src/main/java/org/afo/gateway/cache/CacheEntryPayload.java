package org.afo.gateway.cache;

import lombok.Data;

@Data
public class CacheEntryPayload {
    private Long entryId;
    private String responseText;
    private Long hitCount;
    private Integer tokenCount;
}
