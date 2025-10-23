package com.std.cuit.model.DTO;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class ResponseFormat {
    @JSONField(name = "type")
    private String type = "text";
} 