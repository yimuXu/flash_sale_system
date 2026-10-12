package com.yimu.flash_sale_system.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.yimu.flash_sale_system.dto.SeckillResponse;
import com.yimu.flash_sale_system.exception.RateLimitedException;
import com.yimu.flash_sale_system.service.SeckillService;
import com.yimu.flash_sale_system.service.OrderService;


@RestController 
public class SeckillController {
    // Inject necessary services
    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    // Define endpoints for seckill-related operations

    @PostMapping("/seckill/{productId}/{userId}")
    public ResponseEntity<SeckillResponse> startSeckill(@PathVariable Long productId, @PathVariable Long userId) {
        try(Entry entry = SphU.entry("seckill")) {
            String orderNo = seckillService.seckill(productId, userId);
            SeckillResponse response = new SeckillResponse(orderNo, "PENDING");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (BlockException e) {
            throw new RateLimitedException("System busy, please try again later");
        }
    }

    @GetMapping ("/seckill/result/{orderNo}")
    public ResponseEntity<String> getSeckillResult(@PathVariable String orderNo) {
        String result = seckillService.getSeckillStatus(orderNo);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(404).build();
    }

    // Preheat the product for seckill
    @PostMapping("/admin/seckill/{productId}/preheat")
    public ResponseEntity<Void> preheatSeckill(@PathVariable Long productId) {
        seckillService.preheatProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
