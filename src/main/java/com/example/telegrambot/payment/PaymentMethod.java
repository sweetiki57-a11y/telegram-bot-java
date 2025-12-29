package com.example.telegrambot.payment;

/**
 * Payment method interface (Strategy pattern)
 */
public interface PaymentMethod {
    /**
     * Process payment transaction
     * @param orderId Order ID
     * @param amount Payment amount
     * @return true if payment successful, false otherwise
     */
    boolean processPayment(String orderId, double amount);
    
    /**
     * Get payment method name
     * @return Payment method name
     */
    String getMethodName();
    
    /**
     * Get payment method display emoji
     * @return Emoji for payment method
     */
    String getEmoji();
}

