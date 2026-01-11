package com.example.telegrambot.trading;

/**
 * Решение о торговле
 */
public class TradingDecision {
    public enum Action {
        BUY,    // Покупка
        SELL,   // Продажа
        HOLD    // Удержание позиции
    }
    
    private final Action action;
    private final double amount;      // Количество для покупки/продажи
    private final double price;       // Цена сделки
    private final String reason;      // Причина решения
    private final double confidence; // Уверенность в решении (0.0 - 1.0)
    
    public TradingDecision(Action action, double amount, double price, String reason, double confidence) {
        this.action = action;
        this.amount = amount;
        this.price = price;
        this.reason = reason;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Ограничиваем 0-1
    }
    
    public Action getAction() {
        return action;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public double getPrice() {
        return price;
    }
    
    public String getReason() {
        return reason;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public boolean shouldExecute() {
        return action != Action.HOLD && confidence > 0.5;
    }
    
    @Override
    public String toString() {
        return String.format("%s %.4f @ %.2f (confidence: %.2f%%) - %s", 
            action, amount, price, confidence * 100, reason);
    }
}
