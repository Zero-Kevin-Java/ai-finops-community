package org.afo.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.afo.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("afo_whitelist_recommendations")
public class WhitelistRecommendation extends TenantEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String miningSource;

    private String requestPath;

    private String keyword;

    private String recommendedPattern;

    private String matchType;

    private Integer requestCount;

    private BigDecimal avgConfidence;

    private String dominantModel;

    private String reason;

    private String status;

    private Long acceptedRuleId;

    @TableLogic
    private String delFlag;

    private LocalDateTime expiredAt;

    private String remark;
}
