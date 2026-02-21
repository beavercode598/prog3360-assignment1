package com.prog3360.order_service.controller;

import com.prog3360.order_service.client.ProductClient;
import com.prog3360.order_service.dto.CreateOrderRequest;
import com.prog3360.order_service.dto.ProductDto;
import com.prog3360.order_service.flags.FeatureFlagService;
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
    private final FeatureFlagService featureFlagService;

    public OrderController(OrderRepository repo, ProductClient productClient, FeatureFlagService featureFlagService) {
        this.repo = repo;
        this.productClient = productClient;
        this.featureFlagService = featureFlagService;
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
ProductDto product;
try {
    product = productClient.getProductById(req.getProductId());
} catch (Exception e) {
    // Covers 404/connection errors from RestTemplate/Feign/etc.
    return ResponseEntity.badRequest().body("Product not found.");
}

if (product == null) {
    return ResponseEntity.badRequest().body("Product not found.");
}

        if (product.getQuantity() < req.getQuantity()) {
            return ResponseEntity.badRequest().body("Insufficient product quantity.");
        }

        double totalPrice = product.getPrice() * req.getQuantity();

        // Feature flag: bulk-order-discount (15% off when quantity > 5)
        System.out.println("[flags] bulk-order-discount=" + featureFlagService.isEnabled("bulk-order-discount"));
        if (featureFlagService.isEnabled("bulk-order-discount") && req.getQuantity() > 5) {
            totalPrice = round2(totalPrice * 0.85);
        } else {
            totalPrice = round2(totalPrice);
        }

        Order order = new Order(req.getProductId(), req.getQuantity(), totalPrice, "CREATED");
        Order savedOrder = repo.save(order);

        // Feature flag: order-notifications (log when order created)
        if (featureFlagService.isEnabled("order-notifications")) {
            System.out.println("[order-notifications] Order created: id=" + savedOrder.getId()
                    + " productId=" + savedOrder.getProductId()
                    + " quantity=" + savedOrder.getQuantity()
                    + " totalPrice=" + savedOrder.getTotalPrice());
        }

        return ResponseEntity.ok(savedOrder);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}