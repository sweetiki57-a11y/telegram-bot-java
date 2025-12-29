package com.example.telegrambot.payment;

import java.util.Random;

/**
 * Cryptocurrency payment method implementation
 */
public class CryptoPaymentMethod implements PaymentMethod {
    
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
            
            // Simulate blockchain transaction verification
            // In production, this would integrate with blockchain API
            
            Random random = new Random();
            boolean hasError = random.nextInt(100) < 5; // 5% error probability for testing
            
            if (hasError) {
                System.out.println("Blockchain error for order: " + orderId);
                return false;
            }
            
            System.out.println("Blockchain transaction successful for order: " + orderId + ", amount: " + amount);
            return true;
            
        } catch (Exception e) {
            System.err.println("Blockchain processing error: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getMethodName() {
        return "Cryptocurrency";
    }
    
    @Override
    public String getEmoji() {
        return "₿";
    }
}

