package com.example.telegrambot.payment;

/**
 * Factory for creating payment methods (Factory pattern)
 */
public class PaymentMethodFactory {
    
    public enum PaymentType {
        CRYPTO,
        STARS
    }
    
    /**
     * Create payment method by type
     * @param type Payment type
     * @return Payment method instance
     */
    public static PaymentMethod create(PaymentType type) {
        switch (type) {
            case CRYPTO:
                return new CryptoPaymentMethod();
            case STARS:
                return new StarsPaymentMethod();
            default:
                throw new IllegalArgumentException("Unknown payment type: " + type);
        }
    }
    
    /**
     * Create payment method by string name
     * @param typeName Payment type name
     * @return Payment method instance
     */
    public static PaymentMethod create(String typeName) {
        try {
            PaymentType type = PaymentType.valueOf(typeName.toUpperCase());
            return create(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown payment type: " + typeName);
        }
    }
}

