package com.example.telegrambot.payment;

/**
 * Payment processor using Strategy pattern
 */
public class PaymentProcessor {
    private PaymentMethod paymentMethod;
    
    public PaymentProcessor(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    /**
     * Process payment with current payment method
     * @param orderId Order ID
     * @param amount Payment amount
     * @return Payment result
     */
    public PaymentResult process(String orderId, double amount) {
        boolean success = paymentMethod.processPayment(orderId, amount);
        return new PaymentResult(success, orderId, amount, paymentMethod);
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
}

