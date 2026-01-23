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

        // 시나리오 2: 데이터베이스 연결 실패
        if (orderId.startsWith("DB_FAIL_")) {
            log.error("❌ [DB 연결 실패] 주문 {} - 데이터베이스 타임아웃", orderId);
            throw new OrderProcessingException("Database connection timeout");
        }

        // 시나리오 3: 외부 API 호출 실패 (예: 결제 게이트웨이)
        if (orderId.startsWith("API_FAIL_")) {
            log.error("❌ [외부 API 실패] 주문 {} - 결제 게이트웨이 응답 없음", orderId);
            throw new OrderProcessingException("Payment gateway timeout");
        }

        // 시나리오 4: 비즈니스 로직 실패 (재고 부족)
        if (orderId.startsWith("NO_STOCK_")) {
            log.error("❌ [재고 부족] 주문 {} - 요청 수량: {}, 가용 재고: 0", orderId, event.quantity());
            throw new OrderProcessingException("Insufficient stock");
        }

        // 시나리오 5: 중복 처리 감지
        if (DUPLICATE_DETECT.contains(orderId)) {
            log.warn("⚠️  [중복 처리 감지] 주문 {} - 이미 처리됨!", orderId);
        } else {
            DUPLICATE_DETECT.add(orderId);
        }

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
