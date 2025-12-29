package com.example.telegrambot.payment;

/**
 * Payment processing result
 */
public class PaymentResult {
    private final boolean success;
    private final String orderId;
    private final double amount;
    private final PaymentMethod paymentMethod;
    private String errorMessage;
    
    public PaymentResult(boolean success, String orderId, double amount, PaymentMethod paymentMethod) {
        this.success = success;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
    
    public PaymentResult(boolean success, String orderId, double amount, PaymentMethod paymentMethod, String errorMessage) {
        this(success, orderId, amount, paymentMethod);
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}

