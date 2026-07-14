package org.afo.strategy.support;

import org.afo.common.core.utils.StringUtils;
import org.afo.common.tenant.helper.TenantHelper;
import org.springframework.stereotype.Component;

/**
 * Resolves the tenant used by strategy control-plane operations.
 */
@Component
public class StrategyTenantResolver {

    public static final String DEFAULT_TENANT_ID = "000000";

    public String resolve(String tenantId) {
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }
        String currentTenantId = TenantHelper.getTenantId();
        return StringUtils.isNotBlank(currentTenantId) ? currentTenantId : DEFAULT_TENANT_ID;
    }
}
