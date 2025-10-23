package com.std.cuit.model.DTO;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @JSONField(name = "content")
    private String content;

    @JSONField(name = "role")
    private String role;
}
