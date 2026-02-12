package org.example.producer.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.common.constants.OrderTopic;
import org.example.common.dto.OrderEvent;
import org.example.common.util.JsonUtils;
import org.example.producer.controller.request.OrderRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaProducer<String, String> kafkaProducer;

    public String createOrder(OrderRequest request) {
        String orderId = request.orderId() != null
            ? request.orderId()
            : UUID.randomUUID().toString();

        var orderEvent = OrderEvent.create(orderId, request.productName(), request.quantity());

        sendOrderEvent(orderId, orderEvent);
        return orderId;
    }

    /**
     * 메시지 발행
     */
    private void sendOrderEvent(String key, OrderEvent event) {
        String orderEvent = JsonUtils.toJson(event);
        ProducerRecord<String, String> record = new ProducerRecord<>(OrderTopic.ORDER_CREATED, key, orderEvent);

        kafkaProducer.send(record, (metadata, ex) -> {
            if (ex == null) {
                log.info("========================================");
                log.info("✅ 메시지 발행 성공!");
                log.info("----------------------------------------");
                log.info("📌 토픽: {}", metadata.topic());
                log.info("📌 파티션: {}", metadata.partition());
                log.info("📌 오프셋: {}", metadata.offset());
                log.info("----------------------------------------");
                log.info("📦 주문 ID: {}", event.orderId());
                log.info("📦 상품명: {}", event.productName());
                log.info("📦 수량: {}", event.quantity());
                log.info("📦 주문 시각: {}", event.createdAt());
                log.info("========================================");
            } else{
                log.error("❌ 메시지 발행 실패: {}", ex.getMessage(), ex);
            }
        });
    }

}
