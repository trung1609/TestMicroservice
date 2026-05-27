package com.trung.orderservice.service.impl;

import com.trung.orderservice.dto.OrderCreateRequest;
import com.trung.orderservice.dto.OrderResponse;
import com.trung.orderservice.dto.ProductResponse;
import com.trung.orderservice.entity.OrderStatus;
import com.trung.orderservice.entity.Orders;
import com.trung.orderservice.event.OrderPlaceEvent;
import com.trung.orderservice.exception.BadRequestException;
import com.trung.orderservice.exception.ResourceNotFoundException;
import com.trung.orderservice.repository.OrderRepository;
import com.trung.orderservice.service.OrderService;
import com.trung.orderservice.service.client.ProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderCreateRequest request) throws Exception {
        ProductResponse productResponse = productClient.getProductById(request.getProductId()).getBody();
        if (productResponse == null) {
            throw new ResourceNotFoundException("Product not found with id: " + request.getProductId());
        }
        if (productResponse.getQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient quantity available for product with id: " + request.getProductId());
        }
        BigDecimal totalPrice = productResponse.getPrice().multiply(
                BigDecimal.valueOf(request.getQuantity())
        );
        Orders orders = new Orders();
        orders.setProductId(request.getProductId());
        orders.setQuantity(request.getQuantity());
        orders.setTotalPrice(totalPrice);
        orders.setStatus(OrderStatus.PENDING);
        orderRepository.save(orders);

        OrderPlaceEvent event = OrderPlaceEvent.builder()
                .orderId(orders.getId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();
        kafkaTemplate.send("order-place-topic", event);
        return OrderResponse.builder()
                .id(orders.getId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .status(orders.getStatus().name())
                .build();
    }
}
