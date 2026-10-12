package com.yimu.flash_sale_system.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import jakarta.annotation.PostConstruct;

@Configuration
public class SentinelConfig {
    // Configure Sentinel rules and settings
    @Value("${seckill.rate-limit.qps:500}")
    private double seckillQps;

    @PostConstruct
    public void initRules() {
        // Initialize Sentinel rules here
        FlowRule rule = new FlowRule("seckill");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS); // according to requirements per sec
        rule.setCount(seckillQps);
        FlowRuleManager.loadRules(List.of(rule));

    }
}
