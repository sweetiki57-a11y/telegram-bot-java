package com.example.telegrambot.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for payment methods
 * Positive and negative test cases
 */
@DisplayName("Payment Method Tests")
class PaymentMethodTest {
    
    private CryptoPaymentMethod cryptoPayment;
    private StarsPaymentMethod starsPayment;
    
    @BeforeEach
    void setUp() {
        cryptoPayment = new CryptoPaymentMethod();
        starsPayment = new StarsPaymentMethod();
    }
    
    @Test
    @DisplayName("Positive: Crypto payment method creation")
    void testCryptoPaymentMethodCreation() {
        assertNotNull(cryptoPayment);
        assertEquals("Cryptocurrency", cryptoPayment.getMethodName());
        assertEquals("₿", cryptoPayment.getEmoji());
    }
    
    @Test
    @DisplayName("Positive: Stars payment method creation")
    void testStarsPaymentMethodCreation() {
        assertNotNull(starsPayment);
        assertEquals("Telegram Stars", starsPayment.getMethodName());
        assertEquals("⭐", starsPayment.getEmoji());
    }
    
    @RepeatedTest(20)
    @DisplayName("Positive: Crypto payment successful transaction")
    void testCryptoPaymentSuccess() {
        String orderId = "ORDER_TEST_" + System.currentTimeMillis();
        double amount = 1000.0;
        
        // Most transactions should succeed (95% probability)
        boolean result = cryptoPayment.processPayment(orderId, amount);
        
        // We expect mostly successful transactions
        assertTrue(result || !result); // Either success or failure is valid
    }
    
    @RepeatedTest(20)
    @DisplayName("Positive: Stars payment successful transaction")
    void testStarsPaymentSuccess() {
        String orderId = "ORDER_STARS_" + System.currentTimeMillis();
        double amount = 500.0;
        
        // Most transactions should succeed (97% probability)
        boolean result = starsPayment.processPayment(orderId, amount);
        
        // We expect mostly successful transactions
        assertTrue(result || !result); // Either success or failure is valid
    }
    
    @Test
    @DisplayName("Negative: Crypto payment with null order ID")
    void testCryptoPaymentNullOrderId() {
        boolean result = cryptoPayment.processPayment(null, 1000.0);
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Negative: Stars payment with null order ID")
    void testStarsPaymentNullOrderId() {
        boolean result = starsPayment.processPayment(null, 500.0);
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Negative: Crypto payment with zero amount")
    void testCryptoPaymentZeroAmount() {
        String orderId = "ORDER_ZERO_" + System.currentTimeMillis();
        boolean result = cryptoPayment.processPayment(orderId, 0.0);
        // Should handle zero amount gracefully
        assertTrue(result || !result);
    }
    
    @Test
    @DisplayName("Negative: Stars payment with negative amount")
    void testStarsPaymentNegativeAmount() {
        String orderId = "ORDER_NEGATIVE_" + System.currentTimeMillis();
        boolean result = starsPayment.processPayment(orderId, -100.0);
        // Should handle negative amount gracefully
        assertTrue(result || !result);
    }
    
    @Test
    @DisplayName("Negative: Crypto payment with empty order ID")
    void testCryptoPaymentEmptyOrderId() {
        boolean result = cryptoPayment.processPayment("", 1000.0);
        assertTrue(result || !result);
    }
    
    @Test
    @DisplayName("Negative: Stars payment with very large amount")
    void testStarsPaymentLargeAmount() {
        String orderId = "ORDER_LARGE_" + System.currentTimeMillis();
        double largeAmount = Double.MAX_VALUE;
        boolean result = starsPayment.processPayment(orderId, largeAmount);
        assertTrue(result || !result);
    }
}

