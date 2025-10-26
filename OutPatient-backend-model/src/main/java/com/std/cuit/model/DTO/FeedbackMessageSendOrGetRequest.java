package com.std.cuit.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackMessageSendOrGetRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 诊断记录ID
     */
    private Long diagId;

    /**
     * 消息内容
     */
    private String content;

}
