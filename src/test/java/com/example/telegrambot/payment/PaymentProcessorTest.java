package com.example.telegrambot.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PaymentProcessor
 * Positive and negative test cases
 */
@DisplayName("Payment Processor Tests")
class PaymentProcessorTest {
    
    private PaymentProcessor cryptoProcessor;
    private PaymentProcessor starsProcessor;
    
    @BeforeEach
    void setUp() {
        cryptoProcessor = new PaymentProcessor(new CryptoPaymentMethod());
        starsProcessor = new PaymentProcessor(new StarsPaymentMethod());
    }
    
    @Test
    @DisplayName("Positive: Process crypto payment successfully")
    void testProcessCryptoPaymentSuccess() {
        String orderId = "ORDER_CRYPTO_" + System.currentTimeMillis();
        double amount = 1500.0;
        
        PaymentResult result = cryptoProcessor.process(orderId, amount);
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getAmount());
        assertNotNull(result.getPaymentMethod());
        assertEquals("Cryptocurrency", result.getPaymentMethod().getMethodName());
    }
    
    @Test
    @DisplayName("Positive: Process stars payment successfully")
    void testProcessStarsPaymentSuccess() {
        String orderId = "ORDER_STARS_" + System.currentTimeMillis();
        double amount = 750.0;
        
        PaymentResult result = starsProcessor.process(orderId, amount);
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getAmount());
        assertNotNull(result.getPaymentMethod());
        assertEquals("Telegram Stars", result.getPaymentMethod().getMethodName());
    }
    
    @Test
    @DisplayName("Positive: Change payment method dynamically")
    void testChangePaymentMethod() {
        PaymentProcessor processor = new PaymentProcessor(new CryptoPaymentMethod());
        assertEquals("Cryptocurrency", processor.getPaymentMethod().getMethodName());
        
        processor.setPaymentMethod(new StarsPaymentMethod());
        assertEquals("Telegram Stars", processor.getPaymentMethod().getMethodName());
    }
    
    @Test
    @DisplayName("Negative: Process payment with null order ID")
    void testProcessPaymentNullOrderId() {
        PaymentResult result = cryptoProcessor.process(null, 1000.0);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }
    
    @Test
    @DisplayName("Negative: Process payment with invalid amount")
    void testProcessPaymentInvalidAmount() {
        String orderId = "ORDER_INVALID_" + System.currentTimeMillis();
        
        PaymentResult result1 = cryptoProcessor.process(orderId, -100.0);
        assertNotNull(result1);
        
        PaymentResult result2 = starsProcessor.process(orderId, 0.0);
        assertNotNull(result2);
    }
    
    @Test
    @DisplayName("Negative: Process payment with empty order ID")
    void testProcessPaymentEmptyOrderId() {
        PaymentResult result = starsProcessor.process("", 500.0);
        
        assertNotNull(result);
        assertEquals("", result.getOrderId());
    }
}

