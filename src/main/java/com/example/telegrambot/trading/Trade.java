package com.example.telegrambot.trading;

import java.time.LocalDateTime;

/**
 * Модель торговой сделки
 */
public class Trade {
    public enum TradeType {
        BUY, SELL
    }
    
    public enum TradeStatus {
        OPEN,      // Открыта
        CLOSED,    // Закрыта
        CANCELLED  // Отменена
    }
    
    private final String id;
    private final String symbol;
    private final TradeType type;
    private final double amount;
    private final double entryPrice;
    private double exitPrice;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private TradeStatus status;
    private final String strategy;
    private double profit; // Прибыль/убыток в процентах
    
    public Trade(String id, String symbol, TradeType type, double amount, 
                double entryPrice, String strategy) {
        this.id = id;
        this.symbol = symbol;
        this.type = type;
        this.amount = amount;
        this.entryPrice = entryPrice;
        this.entryTime = LocalDateTime.now();
        this.status = TradeStatus.OPEN;
        this.strategy = strategy;
        this.profit = 0.0;
    }
    
    public void close(double exitPrice) {
        this.exitPrice = exitPrice;
        this.exitTime = LocalDateTime.now();
        this.status = TradeStatus.CLOSED;
        
        // Вычисляем прибыль/убыток
        if (type == TradeType.BUY) {
            this.profit = ((exitPrice - entryPrice) / entryPrice) * 100;
        } else {
            this.profit = ((entryPrice - exitPrice) / entryPrice) * 100;
        }
    }
    
    public void cancel() {
        this.status = TradeStatus.CANCELLED;
        this.exitTime = LocalDateTime.now();
    }
    
    public String getId() {
        return id;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public TradeType getType() {
        return type;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public double getEntryPrice() {
        return entryPrice;
    }
    
    public double getExitPrice() {
        return exitPrice;
    }
    
    public LocalDateTime getEntryTime() {
        return entryTime;
    }
    
    public LocalDateTime getExitTime() {
        return exitTime;
    }
    
    public TradeStatus getStatus() {
        return status;
    }
    
    public String getStrategy() {
        return strategy;
    }
    
    public double getProfit() {
        return profit;
    }
    
    public boolean isOpen() {
        return status == TradeStatus.OPEN;
    }
    
    public double getCurrentValue(double currentPrice) {
        if (type == TradeType.BUY) {
            return amount * currentPrice;
        } else {
            return amount * entryPrice; // Для продажи возвращаем стоимость входа
        }
    }
    
    @Override
    public String toString() {
        return String.format("Trade[%s] %s %.4f %s @ %.2f (profit: %.2f%%)", 
            id, type, amount, symbol, entryPrice, profit);
    }
}
