package org.afo.strategy.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型准入配置中的模型明细。
 *
 * @author AI-FinOps Team
 * @since 2026-05-11
 */
@Data
public class ModelAccessModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模型编码。 */
    private String modelCode;

    /** 模型展示名称。 */
    private String displayName;

    /** 模型供应商。 */
    private String provider;

    /** 模型类型。 */
    private String modelType;

    /** 模型状态。 */
    private String status;
}
