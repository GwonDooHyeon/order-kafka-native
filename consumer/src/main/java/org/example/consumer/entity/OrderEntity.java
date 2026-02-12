package org.example.consumer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.common.dto.OrderEvent;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    private String productName;

    private int quantity;

    private LocalDateTime createdAt;

    private LocalDateTime consumedAt;

    public static OrderEntity from(OrderEvent event) {
        OrderEntity entity = new OrderEntity();

        entity.orderId = event.orderId();
        entity.productName = event.productName();
        entity.quantity = event.quantity();
        entity.createdAt = event.createdAt();
        entity.consumedAt = LocalDateTime.now();

        return entity;
    }

}
