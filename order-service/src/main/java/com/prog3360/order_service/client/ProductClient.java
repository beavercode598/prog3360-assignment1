package com.prog3360.order_service.client;

import com.prog3360.order_service.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${product.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ProductDto getProductById(Long id) {
        ResponseEntity<ProductDto> res = restClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .toEntity(ProductDto.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            return null;
        }
        return res.getBody();
    }
}
