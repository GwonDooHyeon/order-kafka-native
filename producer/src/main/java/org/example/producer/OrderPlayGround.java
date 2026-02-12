package org.example.producer;

import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.common.constants.OrderTopic;
import org.example.common.dto.OrderEvent;
import org.example.common.util.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderPlayGround {

    private static final Random RANDOM = new Random();
    private static final List<String> PRODUCTS = List.of(
            "MacBook Pro", "MacBook Air", "Mac Mini", "iMac", "iPhone", "iPad", "AirPods", "Apple Watch"
    );

    public static void main(String[] args) {
        var props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        while (true) {
            String orderId = UUID.randomUUID().toString();
            String product = PRODUCTS.get(RANDOM.nextInt(PRODUCTS.size()));
            int quantity = RANDOM.nextInt(5) + 1;

            OrderEvent event = OrderEvent.create(orderId, product, quantity);
            String orderPayload = JsonUtils.toJson(event);

            ProducerRecord<String, String> record = new ProducerRecord<>(OrderTopic.ORDER_CREATED, orderId, orderPayload);

            kafkaProducer.send(record, (metadata, ex) -> {
                if (ex == null) {
                    log.info("[{}] 발송 성공 - 상품: {}, 수량: {}, partition: {}, offset: {}",
                            event.orderId(), event.productName(), event.quantity(),
                            metadata.partition(), metadata.offset());
                } else {
                    log.error("발송 실패: {}", ex.getMessage(), ex);
                }
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                break;
            }
        }

        kafkaProducer.close();
    }
}
