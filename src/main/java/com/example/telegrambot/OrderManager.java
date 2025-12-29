package com.example.telegrambot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager for working with orders
 */
public class OrderManager {
    private static final AtomicLong orderIdCounter = new AtomicLong(1);
    private static final Map<String, Order> orders = new HashMap<>();
    private static final Map<Long, List<String>> userOrders = new HashMap<>();
    
    /**
     * Create new order
     */
    public static String createOrder(long userId, Map<String, Integer> items, double totalAmount) {
        String orderId = "ORDER_" + orderIdCounter.getAndIncrement() + "_" + System.currentTimeMillis();
        
        Order order = new Order(orderId, userId, new HashMap<>(items), totalAmount);
        orders.put(orderId, order);
        
        userOrders.computeIfAbsent(userId, k -> new ArrayList<>()).add(orderId);
        
        return orderId;
    }
    
    /**
     * Get order by ID
     */
    public static Order getOrder(String orderId) {
        return orders.get(orderId);
    }
    
    /**
     * Get all user orders
     */
    public static List<Order> getUserOrders(long userId) {
        List<String> orderIds = userOrders.getOrDefault(userId, new ArrayList<>());
        List<Order> userOrderList = new ArrayList<>();
        
        for (String orderId : orderIds) {
            Order order = orders.get(orderId);
            if (order != null) {
                userOrderList.add(order);
            }
        }
        
        // Sort by creation date (newest first)
        userOrderList.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        
        return userOrderList;
    }
    
    /**
     * Get all orders (for admin)
     */
    public static List<Order> getAllOrders() {
        List<Order> allOrders = new ArrayList<>(orders.values());
        allOrders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return allOrders;
    }
    
    /**
     * Update order status
     */
    public static boolean updateOrderStatus(String orderId, Order.OrderStatus newStatus) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            return true;
        }
        return false;
    }
    
    /**
     * Get order statistics
     */
    public static Map<String, Object> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalOrders = orders.size();
        int pendingOrders = 0;
        int confirmedOrders = 0;
        int processingOrders = 0;
        int shippedOrders = 0;
        int deliveredOrders = 0;
        int cancelledOrders = 0;
        double totalRevenue = 0;
        
        for (Order order : orders.values()) {
            switch (order.getStatus()) {
                case PENDING:
                    pendingOrders++;
                    break;
                case CONFIRMED:
                    confirmedOrders++;
                    break;
                case PROCESSING:
                    processingOrders++;
                    break;
                case SHIPPED:
                    shippedOrders++;
                    break;
                case DELIVERED:
                    deliveredOrders++;
                    totalRevenue += order.getTotalAmount();
                    break;
                case CANCELLED:
                    cancelledOrders++;
                    break;
            }
        }
        
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("confirmedOrders", confirmedOrders);
        stats.put("processingOrders", processingOrders);
        stats.put("shippedOrders", shippedOrders);
        stats.put("deliveredOrders", deliveredOrders);
        stats.put("cancelledOrders", cancelledOrders);
        stats.put("totalRevenue", totalRevenue);
        
        return stats;
    }
    
    /**
     * Format order for display
     */
    public static String formatOrder(Order order, Map<String, Product> products) {
        StringBuilder orderText = new StringBuilder();
        
        orderText.append("📦 Order #").append(order.getId()).append("\n");
        orderText.append("📅 Date: ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        orderText.append("💰 Amount: ").append(String.format("%.2f", order.getTotalAmount())).append("₽\n");
        orderText.append("📊 Status: ").append(getStatusEmoji(order.getStatus())).append(" ").append(getStatusText(order.getStatus())).append("\n\n");
        
        orderText.append("🛍️ Products:\n");
        for (Map.Entry<String, Integer> entry : order.getItems().entrySet()) {
            Product product = products.get(entry.getKey());
            if (product != null) {
                double itemTotal = product.getPrice() * entry.getValue();
                orderText.append("• ").append(product.getName())
                        .append(" x").append(entry.getValue())
                        .append(" = ").append(String.format("%.2f", itemTotal)).append("₽\n");
            }
        }
        
        return orderText.toString();
    }
    
    private static String getStatusEmoji(Order.OrderStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case CONFIRMED: return "✅";
            case PROCESSING: return "🔄";
            case SHIPPED: return "🚚";
            case DELIVERED: return "📦";
            case CANCELLED: return "❌";
            default: return "❓";
        }
    }
    
    private static String getStatusText(Order.OrderStatus status) {
        switch (status) {
            case PENDING: return "Pending";
            case CONFIRMED: return "Confirmed";
            case PROCESSING: return "Processing";
            case SHIPPED: return "Shipped";
            case DELIVERED: return "Delivered";
            case CANCELLED: return "Cancelled";
            default: return "Unknown";
        }
    }
}
