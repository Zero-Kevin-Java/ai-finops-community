package org.afo.common.rabbitmq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 简单任务路由变更事件
 *
 * @author AI-FinOps Team
 * @since 2026-05-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTaskRouteChangeEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tenantId;
}
