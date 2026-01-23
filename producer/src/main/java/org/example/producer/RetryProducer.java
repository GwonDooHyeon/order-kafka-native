package org.example.producer;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.common.constants.KafkaConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Stage 3: 재시도(Retries)와 순서 보장
 * <p>
 * 목표: 재시도 설정과 메시지 순서 보장의 관계 이해
 * <p>
 * 핵심 개념:
 * 1. retries: 전송 실패 시 재시도 횟수 설정
 * 2. retry.backoff.ms: 재시도 간격 (기본값: 100ms)
 * 3. max.in.flight.requests.per.connection: 동시에 진행 가능한 요청 수
 *    - 높을수록 처리량 증가, 하지만 순서 보장 안 함
 *    - 낮을수록 순서 보장, 하지만 처리량 감소
 * 4. enable.idempotence: 중복 전송 방지 및 자동 순서 보장
 * <p>
 * 순서 역전 문제 (Out-of-Order Problem):
 * - 메시지 A, B를 순서대로 보냄 (B가 먼저 전송 완료)
 * - A가 실패해서 재시도 중인데 B가 이미 저장됨
 * - 결과: Broker에는 B, A 순서로 저장 (순서 역전!)
 * <p>
 * 테스트 방법:
 * 1. 토픽 생성
 * $ kafka-topics --create --topic retry-topic \
 *                --partitions 1 --replication-factor 3 \
 *                --bootstrap-server kafka-1:29092
 * <p>
 * 2. 각 시나리오 선택 후 실행
 * <p>
 * 3. Consumer로 메시지 순서 확인
 * $ kafka-console-consumer --bootstrap-server kafka-1:29092 \
 *                          --topic retry-topic \
 *                          --from-beginning
 * <p>
 * 예상 학습 시간: 45-55분
 */
@Slf4j
public class RetryProducer {

    public static void main(String[] args) {
        String topic = "retry-topic";
        int totalMessages = 10000;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.DEFAULT_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // === 순서 보장 없음 (순서 역전 관찰) ===
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // 순서 추적
        AtomicLong maxSequenceReceived = new AtomicLong(-1);
        AtomicInteger outOfOrderCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        Object sequenceLock = new Object();
        StringBuilder receivedOrder = new StringBuilder();

        log.info("========================================");
        log.info("메시지 순서 확인 테스트 시작");
        log.info("총 {}개 메시지 전송 (max.in.flight=5)", totalMessages);
        log.info("========================================");
        log.info("");
        log.info("🛑 NOW! 지금 바로 도커를 중지하세요!");
        log.info("다른 터미널에서 실행:");
        log.info("   docker stop kafka-1");
        log.info("");
        log.info("메시지 전송 중에 Broker를 중지해야 많은 메시지가 pending 상태로 쌓입니다!");
        log.info("========================================");
        log.info("");

        long startTime = System.currentTimeMillis();

        for (int seq = 0; seq < totalMessages; seq++) {
            final String value = "msg-" + seq;
            final int seqNum = seq;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("❌ msg-{} 전송 실패: {}", value, exception.getMessage());
                } else {
                    successCount.getAndIncrement();

                    long currentSequence = Long.parseLong(value.split("-")[1]);

                    synchronized (sequenceLock) {
                        // 도착한 메시지 순서 기록
                        if (receivedOrder.length() > 0) {
                            receivedOrder.append(", ");
                        }
                        receivedOrder.append(currentSequence);

                        // 순서 역전 감지
                        if (currentSequence < maxSequenceReceived.get()) {
                            outOfOrderCount.getAndIncrement();
                            log.warn("⚠️ 순서 역전! 예상: {} 이상, 수신: {}",
                                     maxSequenceReceived.get() + 1, currentSequence);
                        }
                        maxSequenceReceived.set(Math.max(maxSequenceReceived.get(), currentSequence));
                    }
                }
            });

            // 진행률 로깅 (매 1000개마다)
            if ((seq + 1) % 1000 == 0) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("📤 {}개 메시지 전송 완료 ({}ms 경과)", seq + 1, elapsedTime);
            }

            // 약간의 딜레이로 pending 메시지 쌓기
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // ⭐ 중요: 메시지 전송만 요청하고 ACK를 모두 받지 못한 상태 유지
        log.info("========================================");
        log.info("📤 모든 메시지 전송 요청 완료!");
        log.info("⚠️  이 시점에서는 많은 메시지가 아직 pending 상태입니다");
        log.info("========================================");
        log.info("");
        log.info("🔄 이제 flush()를 호출합니다...");
        log.info("Broker가 아직 중지되어 있다면, 재시도가 시작됩니다!");
        log.info("Broker가 이미 시작되었다면:");
        log.info("   docker stop kafka-1  (지금이라도 중지 가능)");
        log.info("   docker start kafka-1 (이후 재시작)");
        log.info("========================================");
        log.info("");

        // flush 호출 시 ACK를 받지 못한 메시지들이 재시도됨
        producer.flush();
        producer.close();

        log.info("========================================");
        log.info("테스트 결과");
        log.info("========================================");
        log.info("성공: {}/{}", successCount.get(), totalMessages);
        log.info("도착 순서: [{}]", receivedOrder);
        log.info("순서 역전 횟수: {}", outOfOrderCount.get());
        if (outOfOrderCount.get() == 0) {
            log.info("✓ 완벽한 순서 보장");
        } else {
            log.warn("✗ 순서 역전 발생! ({}회)", outOfOrderCount.get());
        }
        log.info("========================================");
    }

}
