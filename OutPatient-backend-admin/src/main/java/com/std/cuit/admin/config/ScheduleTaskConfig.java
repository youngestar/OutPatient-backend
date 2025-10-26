package com.std.cuit.admin.config;

import com.std.cuit.service.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

/**
 * 排班任务配置
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduleTaskConfig {


    private ScheduleService scheduleService;

    @Resource
    public void setScheduleService(@Lazy ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * 每周日凌晨2点执行，生成未来14天的排班
     */
    @Scheduled(cron = "${schedule.task.cron:0 0 2 * * SUN}")
    public void generateWeeklySchedule() {
        log.info("开始执行每周排班生成任务...");

        try {
            LocalDate startDate = LocalDate.now().plusDays(1); // 从明天开始
            LocalDate endDate = startDate.plusDays(13); // 生成14天

            int totalGenerated = 0;
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                int count = scheduleService.generateSchedulesForDate(date);
                totalGenerated += count;
            }

            log.info("排班生成任务完成，共生成 {} 个排班记录", totalGenerated);
        } catch (Exception e) {
            log.error("排班生成任务执行失败", e);
        }
    }

    /**
     * 每天凌晨1点检查未来第7天排班
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkAndGenerateSchedule() {
        LocalDate targetDate = LocalDate.now().plusDays(7);
        log.info("检查未来第7天({})的排班情况", targetDate);

        try {
            if (!scheduleService.isScheduleComplete(targetDate)) {
                log.info("检测到日期 {} 排班不完整，开始生成", targetDate);
                scheduleService.generateSchedulesForDate(targetDate);
            }
        } catch (Exception e) {
            log.error("检查排班失败", e);
        }
    }

    /**
     * 生成指定日期的排班
     */
    private void generateDailySchedule(LocalDate scheduleDate) {
        try {
            log.info("开始为日期 {} 生成排班", scheduleDate);

            // 调用Service层的排班生成方法
            int generatedCount = scheduleService.generateSchedulesForDate(scheduleDate);

            if (generatedCount > 0) {
                log.info("为日期 {} 成功生成 {} 个排班", scheduleDate, generatedCount);
            } else {
                log.info("日期 {} 无需生成新排班", scheduleDate);
            }

        } catch (Exception e) {
            log.error("生成日期 {} 的排班失败", scheduleDate, e);
        }
    }

    /**
     * 检查并生成指定日期的排班
     */
    private void checkAndGenerateSchedule(LocalDate targetDate) {
        try {
            log.info("检查日期 {} 的排班完整性", targetDate);

            boolean isComplete = scheduleService.isScheduleComplete(targetDate);

            if (!isComplete) {
                log.warn("日期 {} 排班不完整，开始自动生成", targetDate);
                generateDailySchedule(targetDate);
            } else {
                log.info("日期 {} 排班完整", targetDate);
            }

        } catch (Exception e) {
            log.error("检查日期 {} 的排班失败", targetDate, e);
        }
    }

    /**
     * 检查指定日期的排班数量是否足够
     */
    private boolean checkScheduleCount(LocalDate date) {
        try {
            int actualCount = scheduleService.getScheduleCountByDate(date);
            int expectedCount = getExpectedScheduleCount(date);

            boolean hasEnough = actualCount >= expectedCount * 0.8; // 达到预期80%即认为足够
            log.debug("日期 {} 排班检查: 实际={}, 预期={}, 结果={}",
                    date, actualCount, expectedCount, hasEnough ? "足够" : "不足");

            return hasEnough;
        } catch (Exception e) {
            log.error("检查日期 {} 排班数量失败", date, e);
            return false;
        }
    }

    /**
     * 生成示例排班数据（实际项目中需要替换为真实逻辑）
     */
    private void generateSampleSchedules(LocalDate scheduleDate) {
        // TODO: 替换为实际的排班生成逻辑
        // 示例：为每个门诊和医生生成上午和下午的排班

        log.info("为日期 {} 生成示例排班数据", scheduleDate);

        // 这里可以调用scheduleService.addSchedule来添加具体的排班
        // 需要根据实际的医生、门诊信息来生成
    }

    /**
     * 获取预期排班数量
     */
    private int getExpectedScheduleCount(LocalDate date) {
        // 基于门诊数量、医生数量等计算预期排班数
        // 这里可以进一步优化，比如考虑工作日和周末的不同需求
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 工作日预期排班数量多于周末
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return 30; // 周末预期30个排班
        } else {
            return 60; // 工作日预期60个排班
        }
    }
}