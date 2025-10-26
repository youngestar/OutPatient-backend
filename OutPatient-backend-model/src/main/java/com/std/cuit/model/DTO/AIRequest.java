package com.std.cuit.model.DTO;

import com.alibaba.fastjson2.annotation.JSONField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRequest {

    @JSONField(name = "messages")
    @NotEmpty(message = "消息列表不能为空")
    @Valid
    private List<Message> messages;

    @JSONField(name = "model")
    @Builder.Default
    private String model = "deepseek-chat";

    @JSONField(name = "max_tokens")
    @Builder.Default
    private Integer maxTokens = 2048;

    @JSONField(name = "response_format")
    @NotNull(message = "响应格式不能为空")
    @Valid
    private ResponseFormat responseFormat = new ResponseFormat();

    @JSONField(name = "temperature")
    @Builder.Default
    private Double temperature = 0.3;

    @JSONField(name = "frequency_penalty")
    @Builder.Default
    private Double frequencyPenalty = 0.5;

    @JSONField(name = "presence_penalty")
    @Builder.Default
    private Double presencePenalty = 0.8;

    @JSONField(name = "top_p")
    @Builder.Default
    private Double topP = 0.9;

    @JSONField(name = "stop")
    @Builder.Default
    private String[] stop = new String[]{"请注意："};

    @JSONField(name = "stream")
    @Builder.Default
    private Boolean stream = true;
}