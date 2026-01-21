package org.example.producer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.common.constants.KafkaConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Stage 1: 파티션 3개 + 키 기반 라우팅
 * <p>
 * 목표: 키 값에 따라 메시지가 어떤 파티션으로 가는지 이해
 * - 같은 키를 가진 메시지는 항상 같은 파티션으로 간다 (해시 기반)
 * - 콜백에서 파티션 번호를 확인하여 동작 검증
 * - 메시지 크기와 배치 크기 관계 확인
 */
@Slf4j
public class KeyPartitionProducer {

    public static void main(String[] args) {
        String topic = "multi-partition-topic";

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.DEFAULT_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "10"); // 배치 사이즈에 따라 파티션 나뉨.

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        for (int seq = 0; seq < 10; seq++) {
            String value = topic + " " + seq;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    log.info("offset {}", metadata.offset());
                    log.info("partition {}", metadata.partition());
                } else {
                    log.error("전송 실패", exception);
                }
            });

        }

        producer.close();
    }

}
