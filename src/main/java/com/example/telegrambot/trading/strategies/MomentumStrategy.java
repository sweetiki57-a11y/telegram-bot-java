package com.example.telegrambot.trading.strategies;

import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.TradingStrategy;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Стратегия следования за трендом (Momentum)
 * Покупает при росте, продает при падении
 */
public class MomentumStrategy implements TradingStrategy {
    private static final double MOMENTUM_THRESHOLD = 0.015; // 1.5% изменение (более консервативно)
    private static final double MAX_POSITION_SIZE = 0.2; // Максимум 20% баланса
    private static final double MIN_CONFIDENCE = 0.68; // Минимальная уверенность 68%
    
    @Override
    public TradingDecision makeDecision(String symbol, double currentPrice, 
                                       Map<Long, Double> priceHistory, double balance) {
        if (priceHistory == null || priceHistory.size() < 5) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice, 
                "Недостаточно данных для анализа", 0.0);
        }
        
        // Получаем последние цены
        List<Double> prices = new ArrayList<>(priceHistory.values());
        Collections.sort(prices);
        
        // Вычисляем краткосрочное и долгосрочное среднее
        int shortPeriod = Math.min(3, prices.size() / 3);
        int longPeriod = Math.min(10, prices.size());
        
        double shortMA = prices.subList(prices.size() - shortPeriod, prices.size())
            .stream().mapToDouble(Double::doubleValue).average().orElse(currentPrice);
        double longMA = prices.subList(prices.size() - longPeriod, prices.size())
            .stream().mapToDouble(Double::doubleValue).average().orElse(currentPrice);
        
        // Вычисляем момент
        double momentum = (shortMA - longMA) / longMA;
        
        // Вычисляем скорость изменения
        double priceChange = (currentPrice - prices.get(0)) / prices.get(0);
        
        // Вычисляем волатильность
        double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(currentPrice);
        double variance = prices.stream()
            .mapToDouble(p -> Math.pow(p - mean, 2))
            .average()
            .orElse(0.0);
        double volatility = Math.sqrt(variance) / mean;
        
        // Принимаем решение (только при сильном тренде и низкой волатильности)
        if (momentum > MOMENTUM_THRESHOLD && priceChange > 0.01 && volatility < 0.04) {
            // Сильный восходящий тренд - покупаем
            double amount = Math.min(balance * MAX_POSITION_SIZE / currentPrice, 
                                    balance / currentPrice * 0.3);
            double confidence = Math.min(0.95, 0.68 + Math.abs(momentum) * 18);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.BUY, amount, currentPrice,
                    String.format("ПОКУПКА: Восходящий тренд (момент %.2f%%, изменение %.2f%%, уверенность: %.1f%%)", 
                        momentum * 100, priceChange * 100, confidence * 100), confidence);
            }
        } else if (momentum < -MOMENTUM_THRESHOLD && priceChange < -0.01 && volatility < 0.04) {
            // Сильный нисходящий тренд - шортим
            double amount = balance * MAX_POSITION_SIZE / currentPrice;
            double confidence = Math.min(0.95, 0.68 + Math.abs(momentum) * 18);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.SELL, amount, currentPrice,
                    String.format("ШОРТ: Нисходящий тренд (момент %.2f%%, изменение %.2f%%, уверенность: %.1f%%)", 
                        momentum * 100, priceChange * 100, confidence * 100), confidence);
            }
        }
        
        // Нет четкого тренда - удерживаем
        return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
            String.format("Ожидание сильного тренда (момент: %.2f%%, волатильность: %.2f%%)", 
                momentum * 100, volatility * 100), 0.4);
    }
    
    @Override
    public String getName() {
        return "Momentum (Следование тренду)";
    }
    
    @Override
    public String getDescription() {
        return "Покупает при восходящем тренде, шортит при нисходящем";
    }
}
