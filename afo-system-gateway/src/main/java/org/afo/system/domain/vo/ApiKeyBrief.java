package org.afo.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ApiKeyBrief implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long keyId;
    private String keyAlias;
    private String modelName;
}
