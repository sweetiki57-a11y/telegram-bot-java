package com.example.telegrambot;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Класс для представления заказа
 */
public class Order {
    private String id;
    private long userId;
    private Map<String, Integer> items; // productId -> quantity
    private double totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private String customerInfo;
    
    public enum OrderStatus {
        PENDING,    // Ожидает подтверждения
        CONFIRMED,  // Подтвержден
        PROCESSING, // В обработке
        SHIPPED,    // Отправлен
        DELIVERED,  // Доставлен
        CANCELLED   // Отменен
    }
    
    public Order(String id, long userId, Map<String, Integer> items, double totalAmount) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters
    public String getId() { return id; }
    public long getUserId() { return userId; }
    public Map<String, Integer> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCustomerInfo() { return customerInfo; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(long userId) { this.userId = userId; }
    public void setItems(Map<String, Integer> items) { this.items = items; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCustomerInfo(String customerInfo) { this.customerInfo = customerInfo; }
    
    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
