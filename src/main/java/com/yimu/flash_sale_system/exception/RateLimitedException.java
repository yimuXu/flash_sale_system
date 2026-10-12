package com.yimu.flash_sale_system.exception;

public class RateLimitedException extends RuntimeException {
    public RateLimitedException(String message) {
        super(message);
    }
}
