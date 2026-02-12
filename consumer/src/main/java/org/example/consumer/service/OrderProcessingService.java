package org.example.consumer.service;

import org.springframework.stereotype.Service;
import org.example.common.dto.OrderEvent;
import org.example.consumer.entity.OrderEntity;
import org.example.consumer.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 처리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingService {

    private final OrderRepository orderRepository;

    public void processOrder(OrderEvent event) {
        OrderEntity saved = orderRepository.save(OrderEntity.from(event));

        log.info("✅ [처리완료] 주문 {} - {} x {} DB 저장 (id={})",
            event.orderId(),
            event.productName(),
            event.quantity(),
            saved.getId()
        );
    }

}
