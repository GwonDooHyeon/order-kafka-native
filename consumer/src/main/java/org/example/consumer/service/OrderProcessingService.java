package org.example.consumer.service;

import org.springframework.stereotype.Service;
import org.example.common.dto.OrderEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 처리 서비스
 *
 * 실제 서비스에서는 여기에 다양한 처리를 추가 가능
 * - 이메일/SMS 알림 발송
 * - 재고 차감
 * - 배송 준비
 * - 결제 처리
 */
@Service
@Slf4j
public class OrderProcessingService {

    public void processOrder(OrderEvent event) {
        log.info("🎉 [처리완료] 주문 {} - {} x {} 처리되었습니다!",
            event.orderId(),
            event.productName(),
            event.quantity()
        );
    }

}
