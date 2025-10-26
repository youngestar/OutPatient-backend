package com.std.cuit.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.std.cuit.model.DTO.FeedbackMessageRequest;
import com.std.cuit.model.entity.FeedbackMessage;

import java.util.List;
import java.util.Map;

public interface FeedbackMessageService extends IService<FeedbackMessage> {
    boolean markAllAsRead(Long diagId, Long entityId, Integer role);

    int freeUnreadMessageAndSendToWebSocket(Long userId, Long diagId, Long entityId, Integer role);

    Map<String, Integer> getUnreadMessageCountsByEntityId(Long entityId, Integer role);

    List<FeedbackMessage> getMessagesByDiagId(Long diagId);

    FeedbackMessageRequest sendFeedbackMessage(Long diagId, String content, Integer senderType, Long senderId);
}
