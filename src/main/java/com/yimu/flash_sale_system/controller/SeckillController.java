package com.yimu.flash_sale_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yimu.flash_sale_system.dto.OrderResponse;
import com.yimu.flash_sale_system.service.SeckillService;

@RestController 
public class SeckillController {
    // Inject necessary services
    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    // Define endpoints for seckill-related operations

    @PostMapping("/seckill/{productId}/{userId}")
    public ResponseEntity<OrderResponse> startSeckill(@PathVariable Long productId, @PathVariable Long userId) {
        OrderResponse orderResponse = OrderResponse.fromOrder(seckillService.seckill(productId, userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }
}
