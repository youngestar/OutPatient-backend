package com.std.cuit.service.service.serviceImpl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.common.common.Constants;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.BusinessException;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.entity.Diagnosis;
import com.std.cuit.model.entity.Doctor;
import com.std.cuit.model.entity.FeedbackMessage;
import com.std.cuit.model.entity.Patient;
import com.std.cuit.service.mapper.FeedbackMessageMapper;
import com.std.cuit.service.service.DiagnosisService;
import com.std.cuit.service.service.DoctorService;
import com.std.cuit.service.service.FeedbackMessageService;
import com.std.cuit.service.service.PatientService;
import com.std.cuit.service.utils.redis.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FeedbackMessageServiceImpl extends ServiceImpl<FeedbackMessageMapper, FeedbackMessage> implements FeedbackMessageService {

    // 使用ConcurrentHashMap缓存用户在线状态，避免频繁查询Redis
    private static final Map<Long, Boolean> USER_ONLINE_STATUS = new ConcurrentHashMap<>();

    @Resource
    private RabbitAdmin rabbitAdmin;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private PatientService patientService;

    @Resource
    private DoctorService doctorService;

    @Resource
    private RedisService redisService;

    @Resource
    private DiagnosisService diagnosisService;

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 发送反馈消息
     *
     * @param diagId       诊断ID
     * @param entityId      实体ID
     * @param role   发送者类型（0-患者,1-医生）
     * @return FeedbackMessageRequest
     */
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

    /**
     * 获取未读消息数量
     *
     * @param userId   用户ID
     * @param diagId   诊断ID
     * @return 未读消息数量
     */
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

    /**
     * 获取未读消息数量
     *
     * @param entityId 实体ID
     * @param role     角色类型
     * @return 未读消息数量映射
     */
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

    /**
     * 获取指定诊断ID的所有反馈消息
     *
     * @param diagId 诊断ID
     * @return 所有反馈消息列表
     */
    @Override
    public List<FeedbackMessage> getMessagesByDiagId(Long diagId) {
        ThrowUtils.throwIf(diagId == null
                , ErrorCode.PARAMS_ERROR, "参数不能为空");

        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedbackMessage::getDiagId, diagId)
                .orderByAsc(FeedbackMessage::getCreateTime);

        return this.list(wrapper);

    }

    /**
     * 发送反馈消息
     *
     * @param diagId       诊断ID
     * @param content      消息内容
     * @param senderType   发送者类型（0-患者,1-医生）
     * @param senderId     发送者ID
     * @return 发送结果
     */
    @Override
    public FeedbackMessageRequest sendFeedbackMessage(Long diagId, String content, Integer senderType, Long senderId) {
        // 检查诊断记录是否存在
        Diagnosis diagnosis = diagnosisService.getById(diagId);

        ThrowUtils.throwIf(diagnosis == null
                , ErrorCode.PARAMS_ERROR, "诊断ID不存在");

        // 检查发送者身份是否与诊断记录匹配

        ThrowUtils.throwIf(senderType == 0 && !diagnosis.getPatientId().equals(senderId)
                , ErrorCode.PARAMS_ERROR, "患者身份验证失败");

        ThrowUtils.throwIf(senderType == 1 && !diagnosis.getDoctorId().equals(senderId)
                , ErrorCode.PARAMS_ERROR, "医生身份验证失败");

        // 如果是患者发送消息，检查是否在反馈期内(15天)
        ThrowUtils.throwIf(senderType == 0 && !diagnosisService.isWithinFeedbackPeriod(diagId)
                , ErrorCode.OPERATION_ERROR, "不在反馈期内");

        // 创建反馈消息记录
        FeedbackMessage message = new FeedbackMessage();
        message.setDiagId(diagId)
                .setSenderType(senderType)
                .setSenderId(senderId)
                .setContent(content)
                .setReadStatus(0);


        // 保存消息
        save(message);

        // 获取接收者信息
        Long receiverEntityId; // 接收者实体ID（医生ID或患者ID）
        Long receiverUserId; // 接收者用户ID
        String receiverName;
        if (senderType == 0) {
            // 患者发送，接收者是医生
            receiverEntityId = diagnosis.getDoctorId();
            Doctor doctor = doctorService.getById(diagnosis.getDoctorId());
            receiverName = doctor != null ? doctor.getName() : "未知医生";
            // 获取医生对应的用户ID
            receiverUserId = doctor != null ? doctor.getUserId() : null;
        } else {
            // 医生发送，接收者是患者
            receiverEntityId = diagnosis.getPatientId();
            Patient patient = patientService.getById(diagnosis.getPatientId());
            receiverName = patient != null ? patient.getName() : "未知患者";
            // 获取患者对应的用户ID
            receiverUserId = patient != null ? patient.getUserId() : null;
        }

        // 创建发送者姓名
        String senderName;
        if (senderType == 0) {
            // 患者
            Patient patient = patientService.getById(senderId);
            senderName = patient != null ? patient.getName() : "未知患者";
        } else {
            // 医生
            Doctor doctor = doctorService.getById(senderId);
            senderName = doctor != null ? doctor.getName() : "未知医生";
        }

        // 创建消息DTO
        FeedbackMessageRequest messageRequest = new FeedbackMessageRequest();
        BeanUtils.copyProperties(message, messageRequest);
        messageRequest.setSenderName(senderName);
        messageRequest.setReceiverId(receiverEntityId);
        messageRequest.setReceiverName(receiverName);

        // 检查接收者是否在线并发送消息
        boolean isOnline = isUserOnline(receiverUserId);
        if (isOnline) {
            // 即使用户在线通过WebSocket直接收到消息，也应该更新Redis中的未读消息计数
            // 因为用户可能没有查看该消息
            updateUnreadMessageCountAsync(receiverUserId, diagId, 1);

            // 通过WebSocket点对点发送消息
            // 1. 发送消息内容
            sendMessageToWebSocket(receiverUserId, messageRequest);

            // 2. 发送未读计数更新
            int receiverRole = (senderType == 0) ? 1 : 0; // 如果发送者是患者(0)，接收者是医生(1)，反之亦然
            sendUnreadCountersToWebSocket(receiverUserId, receiverEntityId, receiverRole);
        } else {
            // 确保用户专属队列存在 - 使用用户ID
            ensureUserQueueExists(receiverUserId);

            // 通过RabbitMQ异步发送消息，使用用户ID作为路由键
            String routingKey = Constants.MessageKey.USER_ROUTING_KEY_PREFIX + receiverUserId;
            sendToRabbitMQAsync(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE, routingKey, messageRequest);

            // 异步更新未读消息数量 - 使用用户ID
            updateUnreadMessageCountAsync(receiverUserId, diagId, 1);
        }

        return messageRequest;
    }

    /**
     * 异步发送消息到RabbitMQ
     *
     * @param exchange   交换机名称
     * @param routingKey 路由键
     * @param message    消息对象
     */
    private void sendToRabbitMQAsync(String exchange, String routingKey, Object message) {
        try {
            log.info("异步发送消息到RabbitMQ, exchange: {}, routingKey: {}", exchange, routingKey);
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("RabbitMQ消息发送成功");
        } catch (Exception e) {
            log.error("RabbitMQ消息发送异常", e);
        }
    }

    /**
     * 确保用户专属队列存在
     *
     * @param userId 用户ID
     */
    private void ensureUserQueueExists(Long userId) {
        try {
            // 使用用户ID创建队列名
            String queueName = "user.queue." + userId;

            // 检查队列是否存在
            Properties queueProps = rabbitAdmin.getQueueProperties(queueName);
            if (queueProps == null) {
                log.info("用户队列不存在，创建队列: {}", queueName);

                // 创建队列
                Queue queue = QueueBuilder.durable(queueName)
                        .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                        .withArgument("x-dead-letter-routing-key", "dead.letter.routing.key")
                        .withArgument("x-message-ttl", 30L * 24 * 60 * 60 * 1000) // 30天过期
                        .withArgument("x-queue-mode", "lazy") // 将队列设置为lazy模式
                        .build();
                rabbitAdmin.declareQueue(queue);

                // 创建绑定关系
                String routingKey = Constants.MessageKey.USER_ROUTING_KEY_PREFIX + userId;
                Binding binding = BindingBuilder.bind(queue)
                        .to(new TopicExchange(Constants.MessageKey.FEEDBACK_MESSAGE_QUEUE))
                        .with(routingKey);
                rabbitAdmin.declareBinding(binding);

                log.info("为用户ID: {}创建了专用队列: {}", userId, queueName);
            }
        } catch (Exception e) {
            log.error("创建用户队列异常", e);
        }
    }

    /**
     * 通过WebSocket发送消息给用户
     *
     * @param userId       用户ID
     * @param messageRequest 消息对象
     */
    private void sendMessageToWebSocket(Long userId, FeedbackMessageRequest messageRequest) {
        try {
            // 构造消息对象
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", Constants.WebSocketConstants.TYPE_MESSAGE);

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", messageRequest.getMessageId());
            messageData.put("chatId", messageRequest.getDiagId());
            messageData.put("content", messageRequest.getContent());
            messageData.put("sender", messageRequest.getSenderId());
            messageData.put("senderName", messageRequest.getSenderName());
            messageData.put("senderType", messageRequest.getSenderType());
            messageData.put("timestamp", messageRequest.getCreateTime() != null ?
                    messageRequest.getCreateTime().toInstant(ZoneOffset.UTC).toEpochMilli() : System.currentTimeMillis());

            payload.put("message", messageData);

            // 发送WebSocket消息
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    Constants.WebSocketConstants.FEEDBACK_QUEUE,
                    payload
            );
            log.info("发送消息到WebSocket成功: userId={}, messageId={}", userId, messageRequest.getMessageId());
        } catch (Exception e) {
            log.error("发送消息到WebSocket异常", e);
        }    }


    /**
     * 异步更新未读消息数量
     *
     * @param userId    用户ID
     * @param diagId    诊断ID
     * @param count     更新的数量
     */
    @Async("threadPoolTaskExecutor")
    protected void updateUnreadMessageCountAsync(Long userId, Long diagId, int count) {
        try {
            log.info("更新未读消息数量 - 用户ID: {}, 诊断ID: {}, 数量: {}", userId, diagId, count);
            String redisKey = Constants.RedisKey.MESSAGE_USER + userId;
            RMap<String, Integer> redisMap = redisService.getMap(redisKey);

            // 获取当前未读数
            Integer currentCount = redisMap.get(diagId.toString());
            if (currentCount == null) {
                currentCount = 0;
            }

            // 更新未读数
            int newCount = currentCount + count;
            if (newCount <= 0) {
                // 移除键值对
                redisMap.remove(diagId.toString());
            } else {
                // 设置新的未读数
                redisMap.put(diagId.toString(), newCount);
            }

            // 设置过期时间
            redisMap.expire(Duration.ofDays(30)); // 30天过期

        } catch (Exception e) {
            log.error("更新未读消息数量异常", e);
        }
    }


    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 用户是否在线
     */
    private boolean isUserOnline(Long userId) {
        // 先从缓存中获取
        Boolean cached = USER_ONLINE_STATUS.get(userId);
        if (cached != null) {
            return cached;
        }

        // 缓存中不存在，通过Sa-Token检查
        boolean isOnline = StpUtil.isLogin(userId);

        // 更新缓存
        USER_ONLINE_STATUS.put(userId, isOnline);

        return isOnline;
    }


    /**
     * 通过WebSocket发送未读计数更新给用户
     *
     * @param userId    用户ID
     * @param entityId  实体ID
     * @param role      角色（0-患者,1-医生）
     */
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


    /**
     * 获取用户所有未读消息数量
     *
     * @param entityId 实体ID
     * @param role     角色（0-患者,1-医生）
     * @return 用户所有未读消息数量
     */
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
