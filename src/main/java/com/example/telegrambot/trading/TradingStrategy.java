package com.example.telegrambot.trading;

import java.util.List;
import java.util.Map;

/**
 * Интерфейс торговой стратегии
 */
public interface TradingStrategy {
    /**
     * Принять решение о покупке/продаже
     * @param symbol торговый символ
     * @param currentPrice текущая цена
     * @param priceHistory история цен
     * @param balance текущий баланс
     * @return решение о торговле
     */
    TradingDecision makeDecision(String symbol, double currentPrice, 
                                Map<Long, Double> priceHistory, double balance);
    
    /**
     * Получить название стратегии
     */
    String getName();
    
    /**
     * Получить описание стратегии
     */
    String getDescription();
}
