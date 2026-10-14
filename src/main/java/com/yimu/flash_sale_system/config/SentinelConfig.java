package com.yimu.flash_sale_system.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
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
        DegradeRule degrade = new DegradeRule("seckill-publish");
        degrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);  // According to failure ratio
        degrade.setCount(0.5);              // Failure ratio reaches 50%
        degrade.setMinRequestAmount(20);    // At least 20 requests in the statistical window
        degrade.setStatIntervalMs(10_000);  // Statistical window 10 seconds
        degrade.setTimeWindow(10);          // Keep for 10 seconds after tripping, then send a request to test
        DegradeRuleManager.loadRules(List.of(degrade));
    }
}
