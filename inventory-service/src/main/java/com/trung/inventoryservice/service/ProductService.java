package com.trung.inventoryservice.service;

import com.trung.inventoryservice.dto.ProductResponse;
import com.trung.inventoryservice.event.OrderPlaceEvent;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id) throws Exception;
    String reduceProductQuantity(OrderPlaceEvent event) throws Exception;
}
