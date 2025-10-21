package com.graduation.admin.config;

import com.graduation.service.service.ScheduleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
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

    @Resource
    private ScheduleService scheduleService;

    /**
     * 每两周进行一次排班（每两周的周日凌晨2点执行）
     * 生成未来14天的排班
     */
    @Scheduled(cron = "0 0 2 * * 0") // 每周日凌晨2点执行
    public void scheduleBiweekly() {
        log.info("开始执行排班任务...");

        try {
            // 获取下一个周一的日期
            LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

            // 生成未来14天的排班（2周）
            for (int i = 0; i < 14; i++) {
                LocalDate scheduleDate = nextMonday.plusDays(i);
                generateDailySchedule(scheduleDate);
            }

            log.info("排班任务执行完成，已生成未来14天的排班");
        } catch (Exception e) {
            log.error("排班任务执行失败", e);
        }
    }

    /**
     * 每天凌晨1点检查未来第7天是否已排班
     * 如果未排班则自动生成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkNextSeventhDay() {
        log.info("开始检查未来第7天是否已排班...");

        try {
            LocalDate targetDate = LocalDate.now().plusDays(7);
            checkAndGenerateSchedule(targetDate);
            log.info("未来第7天排班检查完成");
        } catch (Exception e) {
            log.error("检查未来第7天排班失败", e);
        }
    }

    /**
     * 生成指定日期的排班
     */
    private void generateDailySchedule(LocalDate scheduleDate) {
        try {
            // TODO: 这里需要根据实际业务逻辑生成排班
            // 示例逻辑：
            // 1. 获取所有有效的医生和门诊
            // 2. 根据排班规则生成医生的排班计划
            // 3. 调用scheduleService.addSchedule添加排班

            log.debug("为日期 {} 生成排班", scheduleDate);

            // 模拟生成排班数据
            // 实际项目中应该从数据库获取医生和门诊信息
            generateSampleSchedules(scheduleDate);

        } catch (Exception e) {
            log.error("生成日期 {} 的排班失败", scheduleDate, e);
        }
    }

    /**
     * 检查并生成指定日期的排班
     */
    private void checkAndGenerateSchedule(LocalDate targetDate) {
        try {
            // TODO: 检查指定日期是否已经有足够的排班
            // 示例逻辑：
            // 1. 查询指定日期的排班数量
            // 2. 如果排班数量不足，则自动生成

            boolean hasEnoughSchedules = checkScheduleCount(targetDate);

            if (!hasEnoughSchedules) {
                log.warn("日期 {} 排班数量不足，开始自动生成", targetDate);
                generateDailySchedule(targetDate);
            } else {
                log.info("日期 {} 排班数量正常", targetDate);
            }

        } catch (Exception e) {
            log.error("检查日期 {} 的排班失败", targetDate, e);
        }
    }

    /**
     * 检查指定日期的排班数量是否足够
     */
    private boolean checkScheduleCount(LocalDate date) {
        // TODO: 实现实际的排班数量检查逻辑
        // 示例：查询该日期的有效排班数量，判断是否达到预期值

        // 临时返回true，实际项目中需要从数据库查询
        return true;
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
}