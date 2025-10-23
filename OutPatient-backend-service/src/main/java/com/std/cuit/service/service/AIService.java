package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.AiConsultConnectionRequest;
import com.std.cuit.model.DTO.AiConsultRequest;
import com.std.cuit.model.DTO.ConsultSession;
import com.std.cuit.model.entity.AiConsultRecord;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIService extends IService<AiConsultRecord> {
    /**
     * 创建SSE连接，用于流式输出AI回答
     * @param request 连接请求(包含会话ID、预约ID和患者ID)
     * @return SseEmitter对象
     */
    SseEmitter createSseConnection(AiConsultConnectionRequest request);

    /**
     * 处理AI问诊请求，并通过SSE流式返回结果
     * @param request 问诊请求
     * @return 会话ID
     */
    String processAiConsult(AiConsultRequest request);

    /**
     * 保存AI问诊对话记录到数据库（仅在会话结束时调用）
     * @param sessionId 会话ID
     * @return 是否保存成功
     */
    boolean saveConsultRecord(String sessionId);

    /**
     * 获取历史对话会话（从Redis获取）
     * @param sessionId 会话ID
     * @return 对话会话详情
     */
    ConsultSession getConsultSession(String sessionId);

    /**
     * 结束对话会话（将会话状态标记为已结束，并保存到数据库）
     * @param sessionId 会话ID
     * @return 是否结束成功
     */
    boolean endConsultSession(String sessionId);

}
