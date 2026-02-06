package com.prog3360.order_service.controller;

import com.prog3360.order_service.client.ProductClient;
import com.prog3360.order_service.dto.CreateOrderRequest;
import com.prog3360.order_service.dto.ProductDto;
import com.prog3360.order_service.model.Order;
import com.prog3360.order_service.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repo;
    private final ProductClient productClient;

    public OrderController(OrderRepository repo, ProductClient productClient) {
        this.repo = repo;
        this.productClient = productClient;
    }

    // GET /api/orders
    @GetMapping
    public List<Order> getAll() {
        return repo.findAll();
    }

    // GET /api/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/orders
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateOrderRequest req) {
        if (req.getProductId() == null || req.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("Invalid order request.");
        }

        // Call Product Service to validate availability
        ProductDto product = productClient.getProductById(req.getProductId());
        if (product == null) {
            return ResponseEntity.badRequest().body("Product not found.");
        }

        if (product.getQuantity() < req.getQuantity()) {
            return ResponseEntity.badRequest().body("Insufficient product quantity.");
        }

        double totalPrice = product.getPrice() * req.getQuantity();
        Order order = new Order(req.getProductId(), req.getQuantity(), totalPrice, "CREATED");

        return ResponseEntity.ok(repo.save(order));
    }
}
