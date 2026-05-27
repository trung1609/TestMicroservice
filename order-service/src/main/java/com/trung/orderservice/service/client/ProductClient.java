package com.trung.orderservice.service.client;

import com.trung.orderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "INVENTORY-SERVICE")
public interface ProductClient {
    @GetMapping("/api/v1/products/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id);

//    @PutMapping("/api/v1/products/{id}/reduce")
//    public ResponseEntity<String> reduceProductQuantity(@PathVariable Long id, @RequestParam Integer quantity);

}
