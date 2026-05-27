package com.trung.orderservice.event;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPlaceEvent {
    private Long orderId;
    private Long productId;
    private Integer quantity;
}
