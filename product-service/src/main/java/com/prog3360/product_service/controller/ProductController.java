package com.prog3360.product_service.controller;

import com.prog3360.product_service.dto.ProductDto;
import com.prog3360.product_service.flags.FeatureFlagService;
import com.prog3360.product_service.model.Product;
import com.prog3360.product_service.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;
    private final FeatureFlagService featureFlagService;

    public ProductController(ProductRepository repo, FeatureFlagService featureFlagService) {
        this.repo = repo;
        this.featureFlagService = featureFlagService;
    }

    // GET /api/products
    @GetMapping
    public List<Product> getAll() {
        return repo.findAll();
    }

    // GET /api/products/premium (Feature-flagged pricing, always 200)
    @GetMapping("/premium")
public ResponseEntity<List<ProductDto>> getPremiumProducts() {
    boolean premiumOn = featureFlagService.isEnabled("premium-pricing");

   
    System.out.println("premium-pricing enabled? " + premiumOn);

    List<ProductDto> result = repo.findAll().stream()
            .map(p -> new ProductDto(
                    p.getId(),
                    p.getName(),
                    premiumOn ? round2(p.getPrice() * 0.90) : p.getPrice(),
                    p.getQuantity()
            ))
            .collect(Collectors.toList());

    return ResponseEntity.ok(result);
}

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/products
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        // basic sanity
        if (product.getName() == null || product.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (product.getPrice() < 0 || product.getQuantity() < 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repo.save(product));
    }

    // DELETE /api/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}