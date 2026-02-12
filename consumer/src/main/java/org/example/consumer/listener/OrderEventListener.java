package org.example.consumer.listener;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.example.common.constants.OrderTopic;
import org.example.common.dto.OrderEvent;
import org.example.common.util.DateUtils;
import org.example.common.util.JsonUtils;
import org.example.consumer.service.OrderProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Kafka 메시지 수신 리스너 (수동 폴링 방식)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private static final List<String> TOPICS = List.of(OrderTopic.ORDER_CREATED);

    private volatile boolean running = true;

    private final KafkaConsumer<String, String> consumer;
    private final OrderProcessingService orderProcessingService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void startPolling() {
        consumer.subscribe(TOPICS);
        log.info("🎧 Kafka Consumer 시작 - 토픽: {}", TOPICS);

        executorService.submit(this::pollLoop);
    }

    private void pollLoop() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        handleOrderEvent(record);
                    } catch (Exception e) {
                        // 에러 발생 시에도 자동 커밋 때문에 offset이 커밋됨
                        // -> 메시지가 유실됨 (재처리 불가)
                        log.error("❌ 메시지 처리 실패 - Partition: {}, Offset: {} | {}",
                                  record.partition(), record.offset(), e.getMessage());
                        log.warn("⚠️  [자동 커밋] 이 메시지는 이미 커밋되었으므로 재처리 불가능!");
                    }
                }
            }
        } catch (WakeupException e) {
            if (running) {
                throw e;
            }
            log.info("🛑 Consumer wakeup 수신 - 정상 종료 진행");
        } finally {
            consumer.close();
            log.info("🔒 Kafka Consumer 종료 완료");
        }
    }

    /**
     * Kafka 메시지 수신 핸들러
     *
     * @param record Kafka 메시지 레코드 (메타데이터 + 실제 값)
     */
    private void handleOrderEvent(ConsumerRecord<String, String> record) {
        OrderEvent event = JsonUtils.toObject(record.value(), OrderEvent.class);

        if (event == null) {
            log.warn("⚠️ JSON 파싱 실패: {}", record.value());
            return;
        }

        log.info("========================================");
        log.info("📨 메시지 수신!");
        log.info("----------------------------------------");
        log.info("📌 토픽: {}", record.topic());
        log.info("📌 파티션: {}", record.partition());
        log.info("📌 오프셋: {}", record.offset());
        log.info("📌 키: {}", record.key());
        log.info("----------------------------------------");
        log.info("📦 주문 ID: {}", event.orderId());
        log.info("📦 상품명: {}", event.productName());
        log.info("📦 수량: {}", event.quantity());
        log.info("📦 주문시간: {}", DateUtils.formatDateTime(event.createdAt()));
        log.info("========================================");

        // 비즈니스 로직 처리 (예외 발생 가능)
        orderProcessingService.processOrder(event);
    }

    @PreDestroy
    public void shutdown() {
        log.info("🛑 Kafka Consumer 종료 시작");
        running = false;
        consumer.wakeup();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
