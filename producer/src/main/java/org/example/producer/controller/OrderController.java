package org.example.producer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.producer.controller.request.OrderRequest;
import org.example.producer.controller.response.OrderResponse;
import org.example.producer.service.OrderService;

import lombok.RequiredArgsConstructor;

/**
 * 주문 API 컨트롤러
 *
 * REST API를 통해 주문을 생성하고 Kafka로 이벤트 발행
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성
     *
     * 사용법:
     * curl -X POST http://localhost:8080/api/orders \
     * -H "Content-Type: application/json" \
     * -d '{"productName":"맥북 프로","quantity":1}'
     *
     * @param request 주문 요청 정보
     * @return 주문 결과 메시지
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        String orderId = orderService.createOrder(request);
        return ResponseEntity.ok(new OrderResponse(orderId, "주문 이벤트가 Kafka로 발행되었습니다."));
    }

}
