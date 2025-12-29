package com.example.telegrambot.payment;

import java.util.Random;

/**
 * Telegram Stars payment method implementation
 */
public class StarsPaymentMethod implements PaymentMethod {
    
    @Override
    public boolean processPayment(String orderId, double amount) {
        try {
            // Validate input
            if (orderId == null || orderId.isEmpty()) {
                return false;
            }
            
            if (amount <= 0) {
                return false;
            }
            
            // Simulate Telegram Stars payment processing
            // In production, this would use Telegram Stars API
            
            Random random = new Random();
            boolean hasError = random.nextInt(100) < 3; // 3% error probability for testing
            
            if (hasError) {
                System.out.println("Telegram Stars payment error for order: " + orderId);
                return false;
            }
            
            System.out.println("Telegram Stars payment successful for order: " + orderId + ", amount: " + amount);
            return true;
            
        } catch (Exception e) {
            System.err.println("Telegram Stars processing error: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getMethodName() {
        return "Telegram Stars";
    }
    
    @Override
    public String getEmoji() {
        return "⭐";
    }
}

