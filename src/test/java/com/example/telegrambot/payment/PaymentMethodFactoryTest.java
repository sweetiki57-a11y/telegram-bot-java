package com.example.telegrambot.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PaymentMethodFactory
 * Positive and negative test cases
 */
@DisplayName("Payment Method Factory Tests")
class PaymentMethodFactoryTest {
    
    @Test
    @DisplayName("Positive: Create crypto payment method")
    void testCreateCryptoPayment() {
        PaymentMethod method = PaymentMethodFactory.create(PaymentMethodFactory.PaymentType.CRYPTO);
        
        assertNotNull(method);
        assertInstanceOf(CryptoPaymentMethod.class, method);
        assertEquals("Cryptocurrency", method.getMethodName());
    }
    
    @Test
    @DisplayName("Positive: Create stars payment method")
    void testCreateStarsPayment() {
        PaymentMethod method = PaymentMethodFactory.create(PaymentMethodFactory.PaymentType.STARS);
        
        assertNotNull(method);
        assertInstanceOf(StarsPaymentMethod.class, method);
        assertEquals("Telegram Stars", method.getMethodName());
    }
    
    @Test
    @DisplayName("Positive: Create payment method by string name - crypto")
    void testCreateCryptoPaymentByString() {
        PaymentMethod method = PaymentMethodFactory.create("CRYPTO");
        
        assertNotNull(method);
        assertInstanceOf(CryptoPaymentMethod.class, method);
    }
    
    @Test
    @DisplayName("Positive: Create payment method by string name - stars")
    void testCreateStarsPaymentByString() {
        PaymentMethod method = PaymentMethodFactory.create("STARS");
        
        assertNotNull(method);
        assertInstanceOf(StarsPaymentMethod.class, method);
    }
    
    @Test
    @DisplayName("Positive: Create payment method by lowercase string")
    void testCreatePaymentByLowercaseString() {
        PaymentMethod method1 = PaymentMethodFactory.create("crypto");
        PaymentMethod method2 = PaymentMethodFactory.create("stars");
        
        assertNotNull(method1);
        assertNotNull(method2);
        assertInstanceOf(CryptoPaymentMethod.class, method1);
        assertInstanceOf(StarsPaymentMethod.class, method2);
    }
    
    @Test
    @DisplayName("Negative: Create payment method with invalid type")
    void testCreatePaymentInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentMethodFactory.create("INVALID_TYPE");
        });
    }
    
    @Test
    @DisplayName("Negative: Create payment method with null string")
    void testCreatePaymentNullString() {
        assertThrows(NullPointerException.class, () -> {
            PaymentMethodFactory.create((String) null);
        });
    }
    
    @Test
    @DisplayName("Negative: Create payment method with empty string")
    void testCreatePaymentEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentMethodFactory.create("");
        });
    }
}

