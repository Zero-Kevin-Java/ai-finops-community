package org.afo.gateway.classifier;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 分类器心跳检查定时任务
 *
 * @author AI-FinOps Team
 * @since 2026-05-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierHealthChecker {

    private final ClassifierClient classifierClient;

    private volatile boolean lastKnownHealthy = false;
    private volatile Instant lastCheckTime = Instant.EPOCH;
    private volatile String lastErrorMessage = null;

    @Scheduled(fixedRateString = "${afo.gateway.classifier.heartbeat-interval:30000}", initialDelay = 10000)
    public void check() {
        boolean healthy = classifierClient.checkHealth();
        lastKnownHealthy = healthy;
        lastCheckTime = Instant.now();
        lastErrorMessage = healthy ? null : "Classifier health check failed";

        if (healthy) {
            log.info("[ClassifierHealthChecker] Classifier is healthy");
        } else {
            log.warn("[ClassifierHealthChecker] Classifier health check FAILED");
        }
    }

    public boolean isHealthy() {
        return lastKnownHealthy;
    }

    public HealthInfo getHealthInfo() {
        return new HealthInfo(lastKnownHealthy, lastCheckTime, lastErrorMessage);
    }

    @Data
    @RequiredArgsConstructor
    public static class HealthInfo {
        private final boolean healthy;
        private final Instant lastCheckTime;
        private final String lastErrorMessage;
    }
}
