package org.afo.gateway.classifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClassifyResponse {
    @JsonProperty("is_simple")
    private boolean isSimple;

    private double confidence;

    @JsonProperty("model_used")
    private String modelUsed;
}
