package org.afo.gateway.classifier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyRequest {
    private String prompt;
    private String tenantId;
}
