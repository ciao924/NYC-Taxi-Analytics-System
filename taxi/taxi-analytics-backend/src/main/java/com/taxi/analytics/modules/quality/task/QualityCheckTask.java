package com.taxi.analytics.modules.quality.task;

import com.taxi.analytics.modules.quality.service.QualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QualityCheckTask {

    private static final Logger log = LoggerFactory.getLogger(QualityCheckTask.class);
    private final QualityService qualityService;
    
    public QualityCheckTask(QualityService qualityService) {
        this.qualityService = qualityService;
    }

    /**
     * 每天凌晨 2 点执行质量检测
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeQualityCheck() {
        log.info("开始执行质量检测任务");
        try {
            qualityService.executeQualityCheck();
            log.info("质量检测任务执行完成");
        } catch (Exception e) {
            log.error("质量检测任务执行失败", e);
        }
    }

    /**
     * 每小时执行一次快速质量检测
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void executeQuickQualityCheck() {
        log.info("开始执行快速质量检测任务");
        try {
            // 这里可以实现一个快速版本的质量检测
            // 例如只检测关键表或关键指标
            log.info("快速质量检测任务执行完成");
        } catch (Exception e) {
            log.error("快速质量检测任务执行失败", e);
        }
    }
}