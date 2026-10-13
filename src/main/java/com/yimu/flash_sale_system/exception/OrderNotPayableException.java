package com.yimu.flash_sale_system.exception;

public class OrderNotPayableException extends RuntimeException {
    public OrderNotPayableException(String message) {
        super(message);
    }
}
