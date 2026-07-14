package org.afo.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class HeadcountOverviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deptId;

    private String deptName;

    private Integer headcountCap;

    private Long activeCount;

    private Long suspendedCount;

    /**
     * 编制使用率（%）。headcountCap=0（不限制）时返回 0。
     */
    public double getUsageRate() {
        if (headcountCap == null || headcountCap <= 0) return 0;
        long total = (activeCount != null ? activeCount : 0) + (suspendedCount != null ? suspendedCount : 0);
        return Math.round(total * 1000.0 / headcountCap) / 10.0;
    }
}
