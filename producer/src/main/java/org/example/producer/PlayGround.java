package org.example.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
public class PlayGround {

    private static final String TOPIC_NAME = "apple-order-topic";

    public static void main(String[] args) {
        var props  = new Properties();

        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        props.setProperty(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "31000");

//        props.setProperty(ProducerConfig.ACKS_CONFIG, "1");
//        props.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "6");

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        sendOrderMessage(kafkaProducer, TOPIC_NAME, -1,
                1000, 0, 0, false);

        kafkaProducer.close();
    }

    /**
     * Apple 주문 메시지를 Kafka로 전송하는 메소드
     *
     * @param kafkaProducer 프로듀서 객체
     * @param topicName 토픽명
     * @param iterCount 반복 횟수 (-1이면 무한 반복)
     * @param interIntervalMillis 매 메시지 전송 후 대기 시간 (밀리초)
     * @param intervalMillis intervalCount 건마다 대기할 시간 (밀리초)
     * @param intervalCount 몇 건마다 intervalMillis 만큼 대기할지 설정 (0이면 비활성화)
     * @param sync 동기 전송 여부 (true: 동기, false: 비동기)
     */
    public static void sendOrderMessage(KafkaProducer<String, String> kafkaProducer,
                                        String topicName, int iterCount,
                                        int interIntervalMillis, int intervalMillis,
                                        int intervalCount, boolean sync
    ) {
        AppleOrderMessage appleOrderMessage = new AppleOrderMessage();
        int iterSeq = 0;

        long startTime = System.currentTimeMillis();

        while( iterSeq++ != iterCount ) {
            var orderResult = appleOrderMessage.produceMessage(iterSeq);
            String shop = orderResult.key();
            String message = orderResult.message();

            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, shop, message);
            sendMessage(kafkaProducer, producerRecord, orderResult, sync);

            // intervalCount 만큼 수행 후 intervalMillis만큼 쉬기
            if(intervalCount > 0 && iterSeq % intervalCount == 0) {
                try {
                    log.info("####### IntervalCount:" + intervalCount +
                            " intervalMillis:" + intervalMillis + " #########");
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }

            // 한건 한건씩 쉬는것
            if(interIntervalMillis > 0) {
                try {
                    log.info("interIntervalMillis:" + interIntervalMillis);
                    Thread.sleep(interIntervalMillis);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }

        }
        long endTime = System.currentTimeMillis();

        long timeElapsed = endTime - startTime;

        log.info("{} millisecond elapsed for {} iterations", timeElapsed, iterCount);
    }

    public static void sendMessage(KafkaProducer<String, String> kafkaProducer,
                                   ProducerRecord<String, String> producerRecord,
                                   AppleOrderMessage.OrderResult orderResult, boolean sync) {
        if(!sync) {
            kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    log.info("async message:" + orderResult.key() + " partition:" + metadata.partition() +
                            " offset:" + metadata.offset());
                } else {
                    log.error("exception error from broker " + exception.getMessage());
                }
            });
        } else {
            try {
                RecordMetadata metadata = kafkaProducer.send(producerRecord).get();
                log.info("sync message:" + orderResult.key() + " partition:" + metadata.partition() +
                        " offset:" + metadata.offset());
            } catch (ExecutionException e) {
                log.error(e.getMessage());
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }

    }

}
