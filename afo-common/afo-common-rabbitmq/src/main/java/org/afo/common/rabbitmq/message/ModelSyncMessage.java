package org.afo.common.rabbitmq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelSyncMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String action;
    private String tenantId;
    private String modelCode;
    private Long modelId;
    private String oldModelCode;
}
