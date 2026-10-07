package com.yimu.flash_sale_system.service;

public class RedisKeys {
    public static String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    public static String usersKey(Long productId) {
        return "seckill:users:" + productId;
    }

    public static String resultKey(String orderNo) {
        return "seckill:order:" + orderNo;
    }
}
