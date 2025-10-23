package com.std.cuit.service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.common.common.Constants;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.entity.Doctor;
import com.std.cuit.model.entity.FeedbackMessage;
import com.std.cuit.model.entity.Patient;
import com.std.cuit.service.mapper.FeedbackMessageMapper;
import com.std.cuit.service.service.DoctorService;
import com.std.cuit.service.service.FeedbackMessageService;
import com.std.cuit.service.service.PatientService;
import com.std.cuit.service.utils.redis.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FeedbackMessageServiceImpl extends ServiceImpl<FeedbackMessageMapper, FeedbackMessage> implements FeedbackMessageService {

    @Resource
    private PatientService patientService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private RedisService redisService;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead(Long diagId, Long entityId, Integer role) {
        ThrowUtils.throwIf(diagId == null
                || entityId == null
                || role == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");
        LambdaUpdateWrapper<FeedbackMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FeedbackMessage::getDiagId, diagId)
                .eq(FeedbackMessage::getReadStatus, 0);

        // 根据角色确定需要标记的消息
        // 角色: 0-患者, 1-医生, 2-管理员
        if (role == 0) {
            // 患者接收的消息由医生发送(senderType=1)
            updateWrapper.eq(FeedbackMessage::getSenderType, 1)
                    .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", entityId);
        } else if (role == 1) {
            // 医生接收的消息由患者发送(senderType=0)
            updateWrapper.eq(FeedbackMessage::getSenderType, 0)
                    .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", entityId);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的角色类型");
        }

        FeedbackMessage entity = new FeedbackMessage();
        entity.setReadStatus(1); // 1表示已读

        return this.update(entity, updateWrapper);
    }

    @Override
    public int freeUnreadMessageAndSendToWebSocket(Long userId, Long diagId, Long entityId, Integer role) {
        log.info("getUnreadMessageCount userId: {}, diagId: {}", userId, diagId);

        ThrowUtils.throwIf(userId == null
                || diagId == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");

        String redisKey = Constants.RedisKey.MESSAGE_USER + userId;
        Integer unreadMessageCount = redisService.getIntFromMap(redisKey, diagId.toString());
        if(unreadMessageCount != null && unreadMessageCount > 0){
            //删除Redis中的数据
            redisService.getMap(redisKey).remove(diagId.toString());

            // 发送未读计数更新
            sendUnreadCountersToWebSocket(userId, entityId, role);

            return unreadMessageCount;
        }
        return 0;
    }

    @Override
    public Map<String, Integer> getUnreadMessageCountsByEntityId(Long entityId, Integer role) {
        log.info("获取实体的所有未读消息数量映射, entityId: {}, role: {}", entityId, role);

        if (entityId == null || role == null) {
            log.warn("参数错误: entityId={}, role={}", entityId, role);
            return new HashMap<>();
        }

        try {
            // 查询条件
            LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeedbackMessage::getReadStatus, 0); // 未读状态

            // 根据角色类型确定查询条件
            if (role == 0) { // 患者
                // 患者接收的消息是医生发送的(senderType=1)
                wrapper.eq(FeedbackMessage::getSenderType, 1)
                        .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.patient_id = {0}", entityId);
            } else if (role == 1) { // 医生
                // 医生接收的消息是患者发送的(senderType=0)
                wrapper.eq(FeedbackMessage::getSenderType, 0)
                        .exists("SELECT 1 FROM diagnosis d WHERE d.diag_id = feedback_message.diag_id AND d.doctor_id = {0}", entityId);
            } else {
                log.error("无效的角色类型: {}", role);
                return new HashMap<>();
            }

            // 查询所有未读消息
            List<FeedbackMessage> messages = list(wrapper);

            // 按诊断ID分组计数
            Map<String, Integer> result = new HashMap<>();
            for (FeedbackMessage message : messages) {
                String diagId = message.getDiagId().toString();
                result.put(diagId, result.getOrDefault(diagId, 0) + 1);
            }

            return result;
        } catch (Exception e) {
            log.error("获取未读消息数量映射异常", e);
            return new HashMap<>();
        }

    }

    @Override
    public List<FeedbackMessage> getMessagesByDiagId(Long diagId) {
        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");

        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getDiagId, diagId)
                .orderByAsc(FeedbackMessage::getCreateTime);

        return this.list(wrapper);

    }

    private void sendUnreadCountersToWebSocket(Long userId, Long entityId, Integer role) {
        // 获取所有未读计数
        Map<String, Integer> unreadCounts = getAllUnreadMessageCounts(entityId, role);

        // 构造消息对象
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", Constants.WebSocketConstants.TYPE_UNREAD_COUNTER);
        payload.put("counters", unreadCounts);

        // 发送WebSocket消息
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                Constants.WebSocketConstants.FEEDBACK_QUEUE,
                payload
        );
        log.info("发送未读计数更新到WebSocket成功: userId={}", userId);
    }

    private Map<String, Integer> getAllUnreadMessageCounts(Long entityId, Integer role) {
        log.info("获取用户的所有诊断未读消息数量, entityId: {}, role: {}", entityId, role);

        if (entityId == null) {
            log.warn("获取所有未读消息数量参数错误: entityId={}, role={}", null, role);
            return new HashMap<>();
        }

        try {
            // 获取用户ID
            //患者
            Long userId = null;
            if (role == 0){
                Patient patient = patientService.getById(entityId);
                userId = patient.getUserId();
            }
            //医生
            else if (role == 1){
                Doctor doctor = doctorService.getById(entityId);
                userId = doctor.getUserId();
            }
            if (userId == null) {
                log.error("无法获取实体对应的用户ID: entityId={}, role={}", entityId, role);
                return new HashMap<>();
            }

            // 从Redis Hash中获取所有诊断的未读消息数量
            String redisKey = Constants.RedisKey.MESSAGE_USER + userId;
            RMap<String, Integer> redisMap = redisService.getMap(redisKey);

            if (redisMap == null || redisMap.isEmpty()) {
                // 如果Redis中没有数据，则从数据库中查询
                Map<String, Integer> countsFromDb = getUnreadMessageCountsByEntityId(entityId, role);
                if (!countsFromDb.isEmpty()) {
                    // 将数据库查询结果存入Redis
                    if (redisMap != null) {
                        redisMap.putAll(countsFromDb);
                    }
                    if (redisMap != null) {
                        redisMap.expire(Duration.ofDays(30)); // 设置30天过期时间
                    }
                }
                return countsFromDb;
            }

            // 使用HashMap复制RedissonMap的内容，避免直接返回可能导致的并发问题
            return new HashMap<>(redisMap);
        } catch (Exception e) {
            log.error("获取所有未读消息数量异常", e);
            return new HashMap<>();
        }

    }
}
