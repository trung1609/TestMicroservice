package com.trung.inventoryservice.service.impl;

import com.trung.inventoryservice.dto.ProductResponse;
import com.trung.inventoryservice.entity.Product;
import com.trung.inventoryservice.event.OrderPlaceEvent;
import com.trung.inventoryservice.repository.ProductRepository;
import com.trung.inventoryservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getProductName(),
                        product.getQuantity(),
                        product.getPrice()
                ))
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(product -> new ProductResponse(
                        product.getId(),
                        product.getProductName(),
                        product.getQuantity(),
                        product.getPrice()
                ))
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Override
    @KafkaListener(topics = "order-place-topic", groupId = "order-service")
    public String reduceProductQuantity(OrderPlaceEvent event) {
        Product product = productRepository.findById(event.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + event.getProductId()));
        if (product.getQuantity() < event.getQuantity()) {
            throw new RuntimeException("Insufficient quantity available for product with id: " + event.getProductId());
        }
        product.setQuantity(product.getQuantity() - event.getQuantity());
        productRepository.save(product);
        return "Product quantity reduced successfully";
    }
}
