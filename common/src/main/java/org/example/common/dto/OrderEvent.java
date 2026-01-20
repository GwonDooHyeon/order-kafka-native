package org.example.common.dto;

import java.time.LocalDateTime;

public record OrderEvent(
    String orderId,
    String productName,
    int quantity,
    LocalDateTime createdAt
) {

    public static OrderEvent create(String orderId, String productName, int quantity) {
        return new OrderEvent(orderId, productName, quantity, LocalDateTime.now());
    }

}
