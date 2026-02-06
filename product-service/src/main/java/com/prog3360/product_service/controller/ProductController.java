package com.prog3360.product_service.controller;

import com.prog3360.product_service.model.Product;
import com.prog3360.product_service.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    // GET /api/products
    @GetMapping
    public List<Product> getAll() {
        return repo.findAll();
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
