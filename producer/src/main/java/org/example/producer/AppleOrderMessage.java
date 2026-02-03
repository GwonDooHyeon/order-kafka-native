package org.example.producer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class AppleOrderMessage {

    private final Random random = new Random();

    private static final List<String> SHOPS = List.of("서울", "부산", "대구", "인천", "대전");
    private static final List<String> PRODUCTS = List.of(
            "MacBook Pro", "MacBook Air", "Mac Mini", "iMac", "iPhone", "iPad", "AirPods", "Apple Watch"
    );

    public record Order(int orderId, int userId, String shop, String product, int quantity) {
        public String toMessage() {
            return "주문ID:ORD-%04d, 사용자ID:USER-%03d, 수령위치:%s, 상품:%s, 수량:%d, 시간:%s"
                    .formatted(orderId, userId, shop, product, quantity, LocalDateTime.now());
        }
    }

    public record OrderResult(String key, String message) {}

    public OrderResult produceMessage(int seq) {
        String shop = SHOPS.get(random.nextInt(SHOPS.size()));
        String product = PRODUCTS.get(random.nextInt(PRODUCTS.size()));
        int quantity = random.nextInt(5) + 1;

        Order order = new Order(seq, seq, shop, product, quantity);
        return new OrderResult(shop, order.toMessage());
    }
}
