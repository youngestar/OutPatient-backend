package com.std.cuit.service.service.serviceImpl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.std.cuit.common.common.Constants;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.exception.ThrowUtils;
import com.std.cuit.model.DTO.*;
import com.std.cuit.model.VO.AiConsultResponse;
import com.std.cuit.model.entity.AiConsultRecord;
import com.std.cuit.service.mapper.AiConsultRecordMapper;
import com.std.cuit.service.service.AIService;
import com.std.cuit.service.utils.redis.RedisService;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * AI 问诊记录表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Slf4j
@Service
public class AIServiceImpl extends ServiceImpl<AiConsultRecordMapper, AiConsultRecord> implements AIService {

    // SSE连接池，仍使用ConcurrentHashMap（不需要持久化）
    private final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    
    // Redis服务
    @Resource
    private RedisService redisService;

    // WebClient实例，用于发送HTTP请求
    private final WebClient webClient;
    
    /**
     * 构造方法，初始化WebClient和AI常量
     */
    public AIServiceImpl(@Value("${ai-service.api-key}") final String API_KEY,
                         @Value("${ai-service.api-url}") final String API_URL) {
        // 创建HttpClient实例，设置超时时间为3分钟
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 180000) // 连接超时3分钟
                .responseTimeout(Duration.ofMinutes(3)) // 响应超时3分钟
                .doOnConnected(conn -> 
                        conn.addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS)) // 读取超时3分钟
                            .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS))); // 写入超时3分钟

        // 创建WebClient实例
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY) // 替换为实际的API密钥
                .build();
    }
    
    /**
     * 获取Redis中的会话Map（带分布式锁）
     */
    private RMap<String, Object> getSessionMap(String sessionId) {
        String redisKey = Constants.RedisKey.AI_CONSULT_SESSION + sessionId;
        RMap<String, Object> sessionMap = redisService.getMap(redisKey);
        
        // 重置过期时间
        sessionMap.expire(Duration.ofHours(Constants.AIConstants.SESSION_EXPIRE_HOURS));
        
        return sessionMap;
    }
    
    /**
     * 获取分布式锁
     */
    private RLock getSessionLock(String sessionId) {
        String lockKey = Constants.RedisKey.AI_CONSULT_LOCK + sessionId;
        return redisService.getLock(lockKey);
    }
    
    /**
     * 将会话保存到Redis（带分布式锁）
     */
    private void saveSessionToRedis(ConsultSession session) {
        if (session == null || session.getSessionId() == null) {
            return;
        }
        
        RLock lock = getSessionLock(session.getSessionId());
        try {
            // 尝试获取锁，等待5秒，10秒后自动释放
            if (lock.tryLock(Constants.AIConstants.LOCK_WAIT_TIME, Constants.AIConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    RMap<String, Object> sessionMap = getSessionMap(session.getSessionId());
                    sessionMap.put("sessionId", session.getSessionId());
                    sessionMap.put("patientId", session.getPatientId());
                    sessionMap.put("appointmentId", session.getAppointmentId());
                    sessionMap.put("status", session.getStatus());
                    sessionMap.put("messageHistory", JSON.toJSONString(session.getMessageHistory()));
                    sessionMap.put("version", session.getVersion() + 1); // 增加版本号
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取会话锁失败: {}", session.getSessionId());
            }
        } catch (InterruptedException e) {
            log.error("保存会话到Redis时被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 从Redis获取会话（带分布式锁）
     */
    private ConsultSession getSessionFromRedis(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        
        RLock lock = getSessionLock(sessionId);
        try {
            if (lock.tryLock(Constants.AIConstants.LOCK_WAIT_TIME, Constants.AIConstants.LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    RMap<String, Object> sessionMap = getSessionMap(sessionId);
                    if (sessionMap.isEmpty()) {
                        return null;
                    }
                    
                    List<MessageRecord> messageHistory = new ArrayList<>();
                    String messageHistoryJson = (String) sessionMap.get("messageHistory");
                    if (messageHistoryJson != null) {
                        messageHistory = JSON.parseArray(messageHistoryJson, MessageRecord.class);
                    }
                    
                    return ConsultSession.builder()
                            .sessionId(sessionId)
                            .patientId((Long) sessionMap.get("patientId"))
                            .appointmentId((Long) sessionMap.get("appointmentId"))
                            .status((Integer) sessionMap.get("status"))
                            .messageHistory(messageHistory)
                            .version((Integer) sessionMap.getOrDefault("version", 0))
                            .build();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("获取会话锁失败: {}", sessionId);
                return null;
            }
        } catch (InterruptedException e) {
            log.error("从Redis获取会话时被中断: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    @Override
    public SseEmitter createSseConnection(AiConsultConnectionRequest request) {
        // 验证必要参数
        ThrowUtils.throwIf(request.getPatientId() == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        ThrowUtils.throwIf(request.getAppointmentId() == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");
        
        String sessionId = request.getSessionId();
        Long patientId = request.getPatientId();
        boolean isNewSession = false;
        
        // 如果sessionId为空，创建新的会话ID
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            isNewSession = true;
        }
        
        // 创建SSE发射器，设置30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 设置SSE完成、超时和错误时的回调
        final String finalSessionId = sessionId;
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: {}", finalSessionId);
            sseEmitterMap.remove(finalSessionId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE连接超时: {}", finalSessionId);
            sseEmitterMap.remove(finalSessionId);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(AiConsultResponse.builder()
                                .event("error")
                                .error("连接超时")
                                .sessionId(finalSessionId)
                                .build()));
                emitter.complete();
            } catch (IOException e) {
                log.error("发送超时事件失败: {}", e.getMessage());
            }
        });
        
        emitter.onError(e -> {
            log.error("SSE连接发生错误: {}", e.getMessage());
            sseEmitterMap.remove(finalSessionId);
        });
        
        // 将SSE发射器添加到映射中
        sseEmitterMap.put(sessionId, emitter);
        
        // 如果是新会话，则在Redis中初始化一个完整会话，确保后续操作能找到会话
        if (isNewSession) {
            // 创建完整会话并保存到Redis
            ConsultSession session = ConsultSession.builder()
                    .sessionId(sessionId)
                    .patientId(patientId)
                    .appointmentId(request.getAppointmentId())
                    .status(0) // 进行中
                    .version(0) // 初始化版本号
                    .messageHistory(new ArrayList<>())
                    .build();
            
            // 添加系统消息
            MessageRecord systemMessage = MessageRecord.builder()
                    .role("system")
                    .content(createSystemPromptForSession(session))
                    .createTime(LocalDateTime.now())
                    .build();
            session.getMessageHistory().add(systemMessage);
            
            // 保存到Redis
            saveSessionToRedis(session);
        } else {
            // 如果使用现有会话ID，验证会话是否存在并检查关联的患者ID和预约ID
            ConsultSession existingSession = getSessionFromRedis(sessionId);
            if (existingSession == null) {
                existingSession = getConsultSession(sessionId);
            }
            
            ThrowUtils.throwIf(existingSession == null
                    , ErrorCode.NOT_FOUND_ERROR, "无效的会话Id");
            
            // 验证会话关联的患者ID和预约ID是否匹配
            ThrowUtils.throwIf(!patientId.equals(existingSession.getPatientId())
                    , ErrorCode.PARAMS_ERROR, "会话关联的患者ID与请求不匹配");

            ThrowUtils.throwIf(!request.getAppointmentId().equals(existingSession.getAppointmentId())
                    , ErrorCode.PARAMS_ERROR, "会话关联的预约ID与请求不匹配");
        }
        
        // 发送连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(AiConsultResponse.builder()
                            .event("connect")
                            .sessionId(finalSessionId)
                            .content("连接已建立")
                            .build()));
        } catch (IOException e) {
            log.error("发送连接事件失败: {}", e.getMessage());
        }
        
        return emitter;
    }
    
    @Override
    public String processAiConsult(AiConsultRequest request) {
        String sessionId = request.getSessionId();
        Long patientId = request.getPatientId();
        Long appointmentId = request.getAppointmentId();
        String question = request.getQuestion();
        
        // 验证必要参数
        ThrowUtils.throwIf(patientId == null
                , ErrorCode.PARAMS_ERROR, "患者ID不能为空");

        ThrowUtils.throwIf(appointmentId == null
                , ErrorCode.PARAMS_ERROR, "预约ID不能为空");

        ThrowUtils.throwIf(question == null|| question.isEmpty()
                , ErrorCode.PARAMS_ERROR, "问题不能为空");
        
        // 获取或创建会话
        ConsultSession session;
        if (sessionId == null || sessionId.isEmpty()) {
            // 创建新会话
            sessionId = UUID.randomUUID().toString();
            
            session = ConsultSession.builder()
                    .sessionId(sessionId)
                    .patientId(patientId)
                    .appointmentId(appointmentId)
                    .status(0) // 进行中
                    .version(0) // 初始化版本号
                    .messageHistory(new ArrayList<>())
                    .build();
            
            // 添加系统消息
            MessageRecord systemMessage = MessageRecord.builder()
                    .role("system")
                    .content(createSystemPromptForSession(session))
                    .createTime(LocalDateTime.now())
                    .build();
            session.getMessageHistory().add(systemMessage);
        } else {
            // 从Redis获取现有会话
            session = getSessionFromRedis(sessionId);
            if (session == null) {
                // 如果Redis中不存在，从数据库尝试获取
                session = getConsultSession(sessionId);
                ThrowUtils.throwIf(session == null
                        , ErrorCode.NOT_FOUND_ERROR, "无效的会话Id");
            }
            
            // 验证会话关联的患者ID和预约ID是否匹配
            ThrowUtils.throwIf(!patientId.equals(session.getPatientId())
                    , ErrorCode.PARAMS_ERROR, "会话关联的患者ID与请求不匹配");

            ThrowUtils.throwIf(!appointmentId.equals(session.getAppointmentId())
                    , ErrorCode.PARAMS_ERROR, "会话关联的预约ID与请求不匹配");
        }

        // 确保version不为null
        if (session.getVersion() == null) {
            session.setVersion(0);
        }

        // 添加用户消息
        MessageRecord userMessage = MessageRecord.builder()
                .role("user")
                .content(question)
                .createTime(LocalDateTime.now())
                .build();
        session.getMessageHistory().add(userMessage);
        
        // 保存会话到Redis
        saveSessionToRedis(session);
        
        // 为Lambda表达式创建final引用
        final String finalSessionId = sessionId;
        final ConsultSession finalSession = session;
        
        // 异步发送请求并处理响应
        new Thread(() -> {
            try {
                // 从会话历史构建AIRequest
                AIRequest aiRequest = buildAIRequest(finalSession);
                
                // 发送请求
                webClient.post()
                        .uri("/chat/completions")
                        .bodyValue(aiRequest)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .subscribe(
                                // 处理成功响应
                                response -> handleAIResponse(response, finalSession),
                                // 处理错误
                                error -> handleAIError(error, finalSessionId)
                        );
            } catch (Exception e) {
                log.error("处理AI问诊请求异常: {}", e.getMessage());
                // 发送错误事件
                sendErrorEvent(finalSessionId, "处理请求失败: " + e.getMessage());
            }
        }).start();
        
        return sessionId;
    }
    

    /**
     * 根据会话内容动态选择系统提示词（针对心理/抑郁关键词切换到更温和且包含危机处理的提示）
     *
     * 说明/假设：
     * - 如果会话历史中出现“抑郁/沮丧/自杀/想死/无助/绝望”等关键词，认为这是需要更高同理心与危机优先的场景，切换为抑郁专用提示。
     * - 这是一个保守的启发式检测；若你有更可靠的患者标签（例如session中包含isMentalHealth或diagnosisTags），可以改为直接读取该标签以做更精确的路由。
     */
    private String createSystemPromptForSession(ConsultSession session) {
        if (session != null && session.getMessageHistory() != null) {
            try {
                for (MessageRecord mr : session.getMessageHistory()) {
                    String content = mr.getContent();
                    if (content != null) {
                        String lower = content.toLowerCase();
                        if (lower.contains("抑郁") || lower.contains("沮丧") || lower.contains("想死")
                                || lower.contains("自杀") || lower.contains("无助") || lower.contains("绝望")) {
                            return createDepressionPrompt();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("检测心理关键词时出错，使用默认提示词: {}", e.getMessage());
            }
        }
        return createWarmPrompt();
    }

    private String createWarmPrompt() {
        return """
                角色设定：
                - 您是一位拥有执业资格的 AI 全科医生
                - 必须遵循《人工智能医疗助手伦理准则》
                
                交流风格：
                - 使用温和、同理、非评判性的语言；优先倾听并验证患者感受
                - 避免过度医学术语；必要时给出简短易懂的解释
                - 当识别到危机信号时（例如自伤、自杀想法），请按规定进入危机应对流程
                
                行为规范：
                1. 问诊流程：症状确认 → 初步建议 → 就医指引
                2. 必须包含风险提示语句并尊重患者的情绪
                3. 禁用具体药物剂量与处方建议；所有用药建议需提示“请咨询主治医生或药师”
                
                输出格式：
                请在回复开始时用一句温和的确认句落地开场（例如“我能理解你现在很难受”），但不要输出“【安慰开场】”标题。
                【症状分析】...
                【初步判断】（说明不确定性）
                【建议与下一步】（就医/随访/紧急处理）
                【风险提示】（如有立即危险，提示联系当地急救或危机热线）
                """;

    }

    private String createDepressionPrompt() {
        return """
                角色设定：
                - 您是一名具备心理卫生知识的 AI（非替代心理治疗师），在与可能有抑郁或自杀风险的患者交流时使用高度同理、温柔且直接的语言。
                
                首要原则（必须遵守）：
                1) 优先确保患者安全：若检测到自伤/自杀意念或计划，立即引导至危机应对（询问当前是否有自伤计划/手段、是否在近期有实施想法），并建议立刻联系当地急救或危机热线。
                2) 不进行诊断结论或给出处方剂量；任何确诊或处方须由有执照的医疗专业人员完成。
                3) 语句要短、清晰、有同理心，避免刺激性或评判性问句。
                
                交流与输出格式建议：
                【同理开场】（例如：“我能感觉到你现在很难受，这一定很不容易。”）
                【简短核查】（睡眠/食欲/兴趣/能量/自责感/有无自伤想法）
                【风险响应】（如有自伤/自杀想法，给出具体行动指引并建议立即联系急救或近期的可信赖的人）
                【支持性建议】（短期应对策略：安全环境、联系信任的人、放慢呼吸等；并建议尽快联系主治医生或心理医生）
                【注意事项】（再次强调不提供处方、并建议面对面评估）
                """;
    }
    
    /**
     * 从会话历史构建AIRequest
     */
    private AIRequest buildAIRequest(ConsultSession session) {
        // 将MessageRecord列表转换为Message列表
        List<Message> messages = new ArrayList<>();
        for (MessageRecord record : session.getMessageHistory()) {
            Message message = Message.builder()
                    .role(record.getRole())
                    .content(record.getContent())
                    .build();
            messages.add(message);
        }

        // 在最前面插入一条明确的格式要求，强制模型在回答中包含指定小标题
        Message formatInstruction = Message.builder()
                .role("system")
                .content(createResponseFormatInstruction())
                .build();
        messages.add(0, formatInstruction);

        // 创建响应格式
        ResponseFormat responseFormat = new ResponseFormat();
        responseFormat.setType("text");

        // 创建AIRequest
        return AIRequest.builder()
                .messages(messages)
                .model("deepseek-chat")
                .maxTokens(2048)
                .temperature(0.3)
                .frequencyPenalty(0.5)
                .presencePenalty(0.8)
                .topP(0.9)
                .stop(new String[]{"请注意："})
                .stream(true) // 开启流式响应
                .responseFormat(responseFormat)
                .build();
    }

    /**
     * 格式化输出强制说明：确保模型输出包含下列标题（每个标题单独成行，且使用方括号）
     */
    private String createResponseFormatInstruction() {
        return """
                请在最终回复中严格包含以下小标题（每个小标题单独成行，使用中文方括号）：
                【症状分析】
                【初步判断】
                【建议与下一步】
                【风险提示】
                在回复开始时请用一句温和的确认句（例如：“我能理解你现在很难受。”）作为自然开场，但不要输出任何形式的“【安慰开场】”标题。
                在每个标题下提供相应内容。不要省略这些标题，也不要改变标题的文字或括号形式。如需添加额外说明，请放在这些标题之后的单独段落。
                """;
    }

    /**
     * 处理AI响应
     */
    private void handleAIResponse(String response, ConsultSession session) {
        try {
            // 检查是否是结束标记
            if (response.trim().equals("[DONE]")) {
                // 这只是流结束标记，不需要处理
                log.info("接收到流结束标记 [DONE], sessionId: {}", session.getSessionId());
                return;
            }

            // 解析响应JSON
            JSONObject jsonResponse = JSON.parseObject(response);
            
            // 检查是否是数据块
            if (jsonResponse.containsKey("choices")) {
                // 提取AI助手的回复内容
                JSONObject firstChoice = jsonResponse.getJSONArray("choices").getJSONObject(0);
                if (firstChoice.containsKey("delta")) {
                    // 处理流式响应的增量部分
                    JSONObject delta = firstChoice.getJSONObject("delta");
                    String content = delta.getString("content");
                    
                    // 如果有内容，发送给前端
                    if (content != null && !content.isEmpty()) {
                        sendMessageEvent(session.getSessionId(), "assistant", content);
                        
                        // 记录增量内容到临时消息中
                        appendTempMessage(session.getSessionId(), content);
                    }
                    
                    // 检查是否完成
                    String finishReason = firstChoice.getString("finish_reason");
                    if ("stop".equals(finishReason)) {
                        // 对话结束，发送完成事件
                        sendCompleteEvent(session.getSessionId());
                        
                        // 将完整的AI回复添加到会话历史
                        String fullResponse = getTempMessage(session.getSessionId());
                        
                        // 添加到会话历史
                        MessageRecord assistantRecord = MessageRecord.builder()
                                .role("assistant")
                                .content(fullResponse)
                                .createTime(LocalDateTime.now())
                                .build();
                        session.getMessageHistory().add(assistantRecord);
                        
                        // 更新Redis中的会话
                        saveSessionToRedis(session);
                        
                        // 清除临时消息
                        clearTempMessage(session.getSessionId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理AI响应异常: {}", e.getMessage());
            sendErrorEvent(session.getSessionId(), "处理响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 增加临时消息内容（用于累积流式响应的内容）
     */
    private void appendTempMessage(String sessionId, String content) {
        String redisKey = Constants.RedisKey.AI_CONSULT_SESSION + sessionId + ":temp_message";
        String currentContent = redisService.getValue(redisKey);
        if (currentContent == null) {
            currentContent = "";
        }
        redisService.setValue(redisKey, currentContent + content, TimeUnit.HOURS.toSeconds(Constants.AIConstants.SESSION_EXPIRE_HOURS));
    }
    
    /**
     * 获取临时消息内容
     */
    private String getTempMessage(String sessionId) {
        String redisKey = Constants.RedisKey.AI_CONSULT_SESSION + sessionId + ":temp_message";
        String content = redisService.getValue(redisKey);
        return content != null ? content : "";
    }
    
    /**
     * 清除临时消息
     */
    private void clearTempMessage(String sessionId) {
        String redisKey = Constants.RedisKey.AI_CONSULT_SESSION + sessionId + ":temp_message";
        redisService.remove(redisKey);
    }
    
    /**
     * 处理AI错误
     */
    private void handleAIError(Throwable error, String sessionId) {
        log.error("AI请求异常: {}", error.getMessage());
        sendErrorEvent(sessionId, "AI服务异常: " + error.getMessage());
    }
    
    /**
     * 发送消息事件
     */
    private void sendMessageEvent(String sessionId, String role, String content) {
        SseEmitter emitter = sseEmitterMap.get(sessionId);
        if (emitter != null) {
            try {
                AiConsultResponse response = AiConsultResponse.builder()
                        .event("message")
                        .role(role)
                        .content(content)
                        .sessionId(sessionId)
                        .build();
                
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(response));
            } catch (IOException e) {
                log.error("发送消息事件失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 发送完成事件
     */
    private void sendCompleteEvent(String sessionId) {
        SseEmitter emitter = sseEmitterMap.get(sessionId);
        if (emitter != null) {
            try {
                AiConsultResponse response = AiConsultResponse.builder()
                        .event("complete")
                        .sessionId(sessionId)
                        .build();
                
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(response));
            } catch (IOException e) {
                log.error("发送完成事件失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 发送错误事件
     */
    private void sendErrorEvent(String sessionId, String errorMessage) {
        SseEmitter emitter = sseEmitterMap.get(sessionId);
        if (emitter != null) {
            try {
                AiConsultResponse response = AiConsultResponse.builder()
                        .event("error")
                        .error(errorMessage)
                        .sessionId(sessionId)
                        .build();
                
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(response));
            } catch (IOException e) {
                log.error("发送错误事件失败: {}", e.getMessage());
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveConsultRecord(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }
        
        try {
            // 从Redis获取会话
            ConsultSession session = getSessionFromRedis(sessionId);
            if (session == null) {
                log.error("保存会话记录失败: 会话不存在, sessionId: {}", sessionId);
                return false;
            }
            
            // 将对话历史转换为JSON字符串
            String conversationJson = JSON.toJSONString(session.getMessageHistory());
            
            // 处理UUID格式的sessionId转为数字
            Long recordId;
            try {
                recordId = Long.parseLong(sessionId.replaceAll("-", ""), 16);
                recordId = Math.abs(recordId % 10000000000000000L);
            } catch (NumberFormatException e) {
                recordId = (long) sessionId.hashCode();
                if (recordId < 0) {
                    recordId = -recordId;
                }
            }
            
            // 使用乐观锁更新记录
            boolean updated = false;
            int maxRetries = 3;
            int retryCount = 0;
            
            while (!updated && retryCount < maxRetries) {
                // 查询现有记录
                AiConsultRecord record = getOne(new LambdaQueryWrapper<AiConsultRecord>()
                        .eq(AiConsultRecord::getRecordId, recordId));
                
                if (record == null) {
                    // 创建新记录
                    record = new AiConsultRecord();
                    record.setRecordId(recordId);
                    record.setPatientId(session.getPatientId());
                    record.setAppointmentId(session.getAppointmentId());
                    record.setStatus(session.getStatus());
                    record.setConversation(conversationJson);
                    record.setCreateTime(LocalDateTime.now());
                    record.setVersion(0);

                    // 对新记录使用save或insert方法
                    updated = save(record);
                }else {

                    // 更新会话内容和状态
                    record.setConversation(conversationJson);
                    record.setUpdateTime(LocalDateTime.now());

                    // 使用乐观锁更新
                    updated = update(record, new LambdaUpdateWrapper<AiConsultRecord>()
                            .eq(AiConsultRecord::getRecordId, recordId)
                            .eq(AiConsultRecord::getVersion, record.getVersion()));
                }
                if (!updated) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        Thread.sleep(100); // 短暂等待后重试
                    }
                }
            }
            
            if (updated) {
                // 更新Redis中的会话状态
                session.setStatus(1); // 标记为已结束
                saveSessionToRedis(session);
            } else {
                log.error("保存会话记录失败: 更新冲突, sessionId: {}", sessionId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("保存对话记录异常: {}", e.getMessage());
            throw new RuntimeException("保存对话记录失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean endConsultSession(String sessionId) {
        try {
            // 从Redis获取会话
            ConsultSession session = getSessionFromRedis(sessionId);
            if (session == null) {
                log.warn("结束会话失败: 会话不存在, sessionId: {}", sessionId);
                return false;
            }

            // 检查会话是否已经结束
            if (session.getStatus() == 1) {
                log.info("会话已结束, 无需重复结束, sessionId: {}", sessionId);
                return true;
            }

            // 更新会话状态
            session.setStatus(1); // 已结束

            // 保存到数据库（带事务）
            boolean saved = saveConsultRecord(sessionId);

            if (saved) {
                // 保存到Redis（保持会话数据，但标记为已结束）
                saveSessionToRedis(session);

                // 注意：不要从sseEmitterMap中移除SSE连接
                // 因为用户可能还需要查看历史会话

                log.info("成功结束AI问诊会话, sessionId: {}", sessionId);
            } else {
                // 如果保存失败，回滚Redis中的状态
                session.setStatus(0);
                saveSessionToRedis(session);
                throw new RuntimeException("保存会话记录失败");
            }

            return true;
        } catch (Exception e) {
            log.error("结束对话会话异常: {}", e.getMessage());
            throw new RuntimeException("结束会话失败", e);
        }
    }
    
    @Override
    public ConsultSession getConsultSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        
        try {
            return getSessionFromRedis(sessionId);
        } catch (Exception e) {
            log.error("获取对话会话异常: {}", e.getMessage());
            return null;
        }
    }
}
