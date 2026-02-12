package org.example.consumer.service;

import org.springframework.stereotype.Service;
import org.example.common.dto.OrderEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 처리 서비스
 */
@Service
@Slf4j
public class OrderProcessingService {

    public void processOrder(OrderEvent event) {
        log.info("✅ [처리완료] 주문 {} - {} x {} 정상 처리됨",
            event.orderId(),
            event.productName(),
            event.quantity()
        );
    }

}
