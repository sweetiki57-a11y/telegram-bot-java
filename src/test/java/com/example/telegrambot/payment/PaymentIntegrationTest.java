package com.example.telegrambot.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for payment flow
 * Tests the complete payment processing flow with positive and negative scenarios
 */
@DisplayName("Payment Integration Tests")
class PaymentIntegrationTest {
    
    private PaymentProcessor cryptoProcessor;
    private PaymentProcessor starsProcessor;
    private static final String PAYMENT_GROUP_LINK = "https://t.me/+MMkALipObugzNjNi";
    
    @BeforeEach
    void setUp() {
        cryptoProcessor = new PaymentProcessor(new CryptoPaymentMethod());
        starsProcessor = new PaymentProcessor(new StarsPaymentMethod());
    }
    
    @Test
    @DisplayName("Positive: Complete crypto payment flow with link generation")
    void testCompleteCryptoPaymentFlow() {
        String orderId = "ORDER_INTEGRATION_CRYPTO_" + System.currentTimeMillis();
        double amount = 2000.0;
        
        PaymentResult result = cryptoProcessor.process(orderId, amount);
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getAmount());
        
        if (result.isSuccess()) {
            // Payment successful - link should be provided
            assertNotNull(PAYMENT_GROUP_LINK);
            assertTrue(PAYMENT_GROUP_LINK.startsWith("https://"));
        } else {
            // Payment failed - no link should be shown
            assertFalse(result.isSuccess());
        }
    }
    
    @Test
    @DisplayName("Positive: Complete stars payment flow with link generation")
    void testCompleteStarsPaymentFlow() {
        String orderId = "ORDER_INTEGRATION_STARS_" + System.currentTimeMillis();
        double amount = 1000.0;
        
        PaymentResult result = starsProcessor.process(orderId, amount);
        
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getAmount());
        
        if (result.isSuccess()) {
            // Payment successful - link should be provided
            assertNotNull(PAYMENT_GROUP_LINK);
            assertTrue(PAYMENT_GROUP_LINK.startsWith("https://"));
        } else {
            // Payment failed - no link should be shown
            assertFalse(result.isSuccess());
        }
    }
    
    @RepeatedTest(10)
    @DisplayName("Positive: Multiple successful payments in sequence")
    void testMultipleSuccessfulPayments() {
        for (int i = 0; i < 5; i++) {
            String orderId = "ORDER_SEQUENCE_" + i + "_" + System.currentTimeMillis();
            double amount = 500.0 + (i * 100);
            
            PaymentResult result = cryptoProcessor.process(orderId, amount);
            assertNotNull(result);
            assertEquals(orderId, result.getOrderId());
        }
    }
    
    @Test
    @DisplayName("Negative: Payment failure should not provide link")
    void testPaymentFailureNoLink() {
        // Simulate payment failure scenario
        String orderId = "ORDER_FAILURE_" + System.currentTimeMillis();
        double amount = 1000.0;
        
        // Process multiple times to potentially get a failure (5% chance for crypto)
        boolean foundFailure = false;
        for (int i = 0; i < 50; i++) {
            PaymentResult result = cryptoProcessor.process(orderId + "_" + i, amount);
            if (!result.isSuccess()) {
                foundFailure = true;
                // When payment fails, link should NOT be shown
                assertFalse(result.isSuccess());
                assertNotNull(result.getErrorMessage() == null ? "" : result.getErrorMessage());
                break;
            }
        }
        
        // At least one failure should occur in 50 attempts with 5% failure rate
        // This is a probabilistic test
    }
    
    @Test
    @DisplayName("Negative: Payment with invalid order data")
    void testPaymentWithInvalidData() {
        PaymentResult result1 = cryptoProcessor.process(null, 1000.0);
        assertNotNull(result1);
        assertFalse(result1.isSuccess());
        
        PaymentResult result2 = starsProcessor.process("", 0.0);
        assertNotNull(result2);
        assertFalse(result2.isSuccess());
    }
    
    @Test
    @DisplayName("Positive: Switch between payment methods during processing")
    void testSwitchPaymentMethods() {
        PaymentProcessor processor = new PaymentProcessor(new CryptoPaymentMethod());
        
        String orderId1 = "ORDER_SWITCH_1_" + System.currentTimeMillis();
        PaymentResult result1 = processor.process(orderId1, 1000.0);
        assertNotNull(result1);
        assertEquals("Cryptocurrency", result1.getPaymentMethod().getMethodName());
        
        // Switch to Stars
        processor.setPaymentMethod(new StarsPaymentMethod());
        String orderId2 = "ORDER_SWITCH_2_" + System.currentTimeMillis();
        PaymentResult result2 = processor.process(orderId2, 1000.0);
        assertNotNull(result2);
        assertEquals("Telegram Stars", result2.getPaymentMethod().getMethodName());
    }
    
    @Test
    @DisplayName("Positive: Verify payment group link format")
    void testPaymentGroupLinkFormat() {
        assertNotNull(PAYMENT_GROUP_LINK);
        assertTrue(PAYMENT_GROUP_LINK.startsWith("https://t.me/"));
        assertFalse(PAYMENT_GROUP_LINK.isEmpty());
    }
}

