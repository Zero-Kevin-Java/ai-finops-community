package org.afo.llm.domain.vo;

import lombok.Data;

@Data
public class ModelConfigVo {

    private String litellmModel;

    private String apiKey;

    private String apiBase;

    private String protocol;
}
