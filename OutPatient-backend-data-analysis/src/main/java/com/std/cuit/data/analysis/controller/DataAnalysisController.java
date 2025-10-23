package com.std.cuit.data.analysis.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SaCheckRole("doctor")
@Slf4j
@RestController
@RequestMapping("/data-analysis")
public class DataAnalysisController {

    @Resource
    private DataAnalysisService dataAnalysisService;
}
