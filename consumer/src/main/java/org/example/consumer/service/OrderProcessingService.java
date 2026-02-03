package org.example.consumer.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.example.common.dto.OrderEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 처리 서비스 - 실무 실패 시나리오 포함
 *
 * 다양한 실패 케이스를 시뮬레이션:
 * - 데이터베이스 연결 실패
 * - 외부 API 호출 실패
 * - 비즈니스 로직 실패 (재고 부족, 유효성 검사 등)
 */
@Service
@Slf4j
public class OrderProcessingService {

    private static final Set<String> DUPLICATE_DETECT = new HashSet<>();

    public void processOrder(OrderEvent event) throws OrderProcessingException {
        String orderId = event.orderId();

        log.info("주문 처리 시작 - 주문ID: {}", orderId);
        // 정상 처리
        log.info("✅ [처리완료] 주문 {} - {} x {} 정상 처리됨",
            orderId,
            event.productName(),
            event.quantity()
        );
    }

    /**
     * 커스텀 예외 클래스
     */
    public static class OrderProcessingException extends Exception {
        public OrderProcessingException(String message) {
            super(message);
        }
    }

}
