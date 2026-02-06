package com.prog3360.order_service.dto;

public class CreateOrderRequest {
    private Long productId;
    private int quantity;

    public CreateOrderRequest() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
