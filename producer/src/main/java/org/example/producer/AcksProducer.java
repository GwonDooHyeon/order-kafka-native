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
 * Stage 2: acks 설정과 전송 보장
 * <p>
 * 목표: acks 설정에 따른 성능과 안정성 트레이드오프 이해
 * - acks=0: 전송만 하고 확인 안 함 (가장 빠름, 유실 가능)
 * - acks=1: Leader에 저장 확인 (Kafka 3.0 이전 기본값, 균형잡힌 선택)
 * - acks=all (-1): 모든 ISR에 저장 확인 (Kafka 3.0+ 기본값, 가장 안전, 느림)
 * <p>
 * 핵심 학습:
 * - acks=0일 때 offset이 -1로 반환되는 이유
 * - acks 설정에 따른 성능 차이 측정
 * <p>
 * 테스트 방법:
 *
 * 1. 토픽 생성 (기존 토픽 삭제 후 생성)
 * $ kafka-topics.sh --delete --topic acks-topic --bootstrap-server localhost:9092
 * $ kafka-topics.sh --create --topic acks-topic \
 *                   --partitions 3 --replication-factor 3 \
 *                   --bootstrap-server localhost:9092 \
 *                   --config min.insync.replicas=2
 *
 * 2. acks 값을 변경하면서 테스트 (0, 1, all)
 * 3. 각 설정에서 offset과 전송 시간 비교
 * 
 * 4. Consumer로 메시지 확인 (Docker Compose)
 * $ kafka-console-consumer --bootstrap-server kafka-1:29092 \
 *                          --topic acks-topic \
 *                          --property print.key=true \
 *                          --property print.value=true \
 *                          --property print.partition=true \
 *                          --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
 */
@Slf4j
public class AcksProducer {

    public static void main(String[] args) {
        String topic = "acks-topic";
        String acks = "all";

        // - ACKS_CONFIG: "0", "1", "all" 중 선택하여 테스트
        //   * "0": 브로커 응답 없이 전송만 함 (가장 빠름)
        //   * "1": 리더에 저장되면 응답 (Kafka 3.0 이전 기본값, 균형)
        //   * "all", "-1": 모든 ISR에 저장되면 응답 (Kafka 3.0+ 기본값, 가장 안전)

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.DEFAULT_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, acks);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        long startTime = System.currentTimeMillis();
        int totalMessages = 1_000_000;

        // offset 통계 수집
        AtomicLong minOffset = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxOffset = new AtomicLong(Long.MIN_VALUE);
        AtomicInteger offsetNegativeOneCount = new AtomicInteger();
        Object offsetLock = new Object();

        for (int seq = 0; seq < totalMessages; seq++) {
            String value = topic + " " + seq;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

            // 비동기 전송
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("전송 실패", exception);
                } else {
                    long offset = metadata.offset();
                    synchronized (offsetLock) {
                        if (offset == -1) {
                            offsetNegativeOneCount.getAndIncrement();
                        } else {
                            minOffset.set(Math.min(minOffset.get(), offset));
                            maxOffset.set(Math.max(maxOffset.get(), offset));
                        }
                    }
                }
            });
        }

        long flushStartTime = System.currentTimeMillis();
        // 모든 pending 메시지가 실제로 전송될 때까지 대기
        producer.flush();
        long flushEndTime = System.currentTimeMillis();

        log.info("========================================");
        log.info("acks={} | 메시지 {}개 전송 결과", acks, totalMessages);
        log.info("========================================");
        log.info("1) 송신 큐 적재 시간: {}ms", flushStartTime - startTime);
        log.info("2) flush() 대기 시간: {}ms", flushEndTime - flushStartTime);
        log.info("3) 총 전송 시간: {}ms", flushEndTime - startTime);
        log.info("========================================");

        // offset 통계 출력
        log.info("========================================");
        log.info("Offset 통계");
        log.info("========================================");
        log.info("offset=-1 개수: {}", offsetNegativeOneCount);
        if (minOffset.get() != Long.MAX_VALUE) {
            log.info("offset 최솟값: {}", minOffset);
            log.info("offset 최댓값: {}", maxOffset);
            log.info("offset 범위: {} ~ {}", minOffset, maxOffset);
        }
        log.info("========================================");

        producer.close();

        // == 카프카 버전 3이후로는 acks = -1이 기본  값
        // replication.factor=5
        // min.insync.replicas=3

        // 주요 값
        // acks
        // replication.factor
        // min.insync.replicas

        // 0
        // 13:03:12.728 [main] INFO org.example.producer.AcksProducer -- 1) 송신 큐 적재 시간: 1606ms
        // 13:03:12.728 [main] INFO org.example.producer.AcksProducer -- 2) flush() 대기 시간: 210ms
        // 13:03:12.728 [main] INFO org.example.producer.AcksProducer -- 3) 총 전송 시간: 1816ms

        // 1
        // 13:03:30.786 [main] INFO org.example.producer.AcksProducer -- 1) 송신 큐 적재 시간: 1307ms
        // 13:03:30.786 [main] INFO org.example.producer.AcksProducer -- 2) flush() 대기 시간: 407ms
        // 13:03:30.786 [main] INFO org.example.producer.AcksProducer -- 3) 총 전송 시간: 1714ms

        // -1
        // 13:01:45.521 [main] INFO org.example.producer.AcksProducer -- 1) 송신 큐 적재 시간: 2241ms
        // 13:01:45.521 [main] INFO org.example.producer.AcksProducer -- 2) flush() 대기 시간: 1840ms
        // 13:01:45.521 [main] INFO org.example.producer.AcksProducer -- 3) 총 전송 시간: 4081ms

    }

}
