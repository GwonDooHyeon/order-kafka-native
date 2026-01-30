package org.example.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ProducerPlayGround {

    // TODO
    // 1. acks 설정
    //      - acks=0: 전송만 하고 확인 안 함 (유실 가능)
    //      - acks=1: 리더만 확인 (리더 장애 시 유실 가능)
    //      - acks=all: 모든 ISR 확인 (가장 안전, 느림)

    // 2. 재시도 관련
    //      - retries=3                    # 재시도 횟수
    //      - retry.backoff.ms=100         # 재시도 간격
    //      - delivery.timeout.ms=120000   # 전체 전송 타임아웃

    // 3. 배치 설정
    //      - batch.size=16384             # 배치 크기 (bytes)
    //      - linger.ms=5                  # 배치 대기 시간
    //      - buffer.memory=33554432       # 버퍼 메모리

    // 실무에서 겪는 문제
    //  1. 순서 보장 이슈
    //      - 순서 보장이 필요하면
    //      - enable.idempotence=true
    //      - max.in.flight.requests.per.connection=5  # 또는 1
    //  2. 프로듀서가 브로커보다 빠르면 buffer.memory 초과로 블로킹되거나 예외 발생 ??
    //       => 다음걸로 적재가 되는거 아닌지 ?

    // 테스트 시나리오   |          테스트 방법           |      확인 포인트
    //    - 브로커 1대 장애 -> 브로커 1대 kill -> 재시도 동작, 메시지 유실 여부 확인
    //    - 파티션 리더 변경 -> 리더 브로커 재시작 -> 순서 보장, 중복 발생 여부

    // ============================================ //

    // 1. 멱등성(= 중복 제거) 설정 및 테스트
    // 2. 재시도 전략 및 순서보장 설정 및 테스트
    // 3. batch.size 및 linger.ms 설정 및 테스트

    // ============================================ //
    // 브로커 설정
    // ============================================
    private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
    private static final String TOPIC = "p3r3";

    // ============================================
    // [실험 1] acks 설정 - 신뢰성 vs 성능
    // ============================================
    // "0"   : 브로커 응답 안 기다림 (가장 빠름, 유실 가능)
    // "1"   : 리더만 확인 (기본값, 리더 장애 시 유실 가능)
    // "all" : 모든 ISR 복제본 확인 (가장 안전, 느림)
    private static final String ACKS = "1";

    // ============================================
    // [실험 2] 배치 설정 - 처리량 최적화
    // ============================================
    // batch.size: 배치 최대 크기 (bytes), 기본값 16384 (16KB)
    // linger.ms: 배치 대기 시간 (ms), 기본값 0 (즉시 전송)
    // → linger.ms를 높이면 배치에 더 많은 메시지가 쌓임
    private static final int BATCH_SIZE = 16384;
    private static final int LINGER_MS = 0;

    // ============================================
    // [실험 3] 재시도 설정 - 장애 대응
    // ============================================
    // retries: 재시도 횟수, 기본값 2147483647 (무한)
    // retry.backoff.ms: 재시도 간격 (ms), 기본값 100
    // delivery.timeout.ms: 전체 전송 타임아웃, 기본값 120000 (2분)
    // → retries * retry.backoff.ms < delivery.timeout.ms 이어야 함
    private static final int RETRIES = 3;
    private static final int RETRY_BACKOFF_MS = 100;
    private static final int DELIVERY_TIMEOUT_MS = 120000;

    // ============================================
    // [실험 4] 멱등성 설정 - 중복 방지
    // ============================================
    // enable.idempotence=true 시 자동으로:
    //   - acks=all, retries=Integer.MAX_VALUE 강제
    //   - 프로듀서 ID + 시퀀스 번호로 중복 메시지 필터링
    private static final boolean ENABLE_IDEMPOTENCE = false;

    // ============================================
    // [실험 5] 압축 설정 - 네트워크/저장 공간 절약
    // ============================================
    // "none", "gzip", "snappy", "lz4", "zstd"
    // gzip: 높은 압축률, CPU 사용 높음
    // snappy/lz4: 빠른 압축/해제, 적당한 압축률
    // zstd: 좋은 압축률 + 적당한 속도 (권장)
    private static final String COMPRESSION_TYPE = "none";

    // ============================================
    // 테스트 메시지 설정
    // ============================================
    private static final int MESSAGE_COUNT = 10_000;
    private static final boolean USE_KEY = true;  // false면 라운드로빈
    private static final boolean SYNC_SEND = false;  // true면 동기 전송

    public static void main(String[] args) {
        printConfig();
        Properties props = buildProducerConfig();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int seq = 1; seq <= MESSAGE_COUNT; seq++) {
                String key = USE_KEY ? "order-" + (seq % 3) : null;  // 3개의 키로 파티션 분배
                String value = "message-" + seq;

                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);

                if (SYNC_SEND) {
                    sendSync(producer, record, seq);
                } else {
                    sendAsync(producer, record, seq);
                }
            }

            // 비동기 전송 시 버퍼 플러시 대기
            if (!SYNC_SEND) {
                log.info("\n[플러시 대기중...]");
                producer.flush();
            }

            log.info("\n전송 완료!");

        } catch (Exception e) {
            log.error("프로듀서 에러: {}", e.getMessage(), e);
        }
    }

    // == Private == //

    private static void printConfig() {
        log.info("================================================");
        log.info("Kafka Producer 실행");
        log.info("================================================");
        log.info("Topic: {}", TOPIC);
        log.info("acks: {}", ACKS);
        log.info("batch.size: {} bytes", BATCH_SIZE);
        log.info("linger.ms: {} ms", LINGER_MS);
        log.info("retries: {}", RETRIES);
        log.info("retry_backoff: {}", RETRY_BACKOFF_MS);
        log.info("delivery_timeout_ms: {}", DELIVERY_TIMEOUT_MS);
        log.info("enable.idempotence: {}", ENABLE_IDEMPOTENCE);
        log.info("compression.type: {}", COMPRESSION_TYPE);
        log.info("MESSAGE_COUNT: {}", MESSAGE_COUNT);
        log.info("USE_KEY: {}, SYNC_SEND: {}", USE_KEY, SYNC_SEND);
        log.info("================================================\n");
    }

    private static Properties buildProducerConfig() {
        Properties props = new Properties();

        // 필수 설정
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 실험 설정
//        props.put(ProducerConfig.ACKS_CONFIG, ACKS);
//        props.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
//        props.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
//        props.put(ProducerConfig.RETRIES_CONFIG, RETRIES);
//        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, RETRY_BACKOFF_MS);
//        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, DELIVERY_TIMEOUT_MS);
//        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, ENABLE_IDEMPOTENCE);
//        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE);

        return props;
    }

    private static void sendSync(KafkaProducer<String, String> producer,
                                  ProducerRecord<String, String> record, int seq) {
        try {
            RecordMetadata metadata = producer.send(record).get();
            printResult(seq, record.key(), metadata);
        } catch (InterruptedException | ExecutionException e) {
            log.error("[{}] 전송 실패: {}", seq, e.getMessage());
        }
    }

    private static void sendAsync(KafkaProducer<String, String> producer,
                                   ProducerRecord<String, String> record, int seq) {
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                printResult(seq, record.key(), metadata);
            } else {
                log.error("[{}] 전송 실패: {}", seq, exception.getMessage());
            }
        });
    }

    private static void printResult(int seq, String key, RecordMetadata metadata) {
        log.info("[{}] key={} → partition={}, offset={}, timestamp={}",
            seq,
            key == null ? "(null)" : String.format("%-10s", key),
            metadata.partition(),
            metadata.offset(),
            metadata.timestamp());
    }
}
