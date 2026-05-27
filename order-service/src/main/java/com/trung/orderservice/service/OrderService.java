package com.trung.orderservice.service;

import com.trung.orderservice.dto.OrderCreateRequest;
import com.trung.orderservice.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request) throws Exception;
}
