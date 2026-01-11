package com.example.telegrambot.trading.strategies;

import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.TradingStrategy;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Стратегия возврата к среднему (Mean Reversion)
 * Покупает когда цена ниже среднего, продает когда выше
 */
public class MeanReversionStrategy implements TradingStrategy {
    private static final double DEVIATION_THRESHOLD = 0.025; // 2.5% отклонение (более консервативно)
    private static final double MAX_POSITION_SIZE = 0.25; // Максимум 25% баланса
    private static final double MIN_CONFIDENCE = 0.65; // Минимальная уверенность 65%
    
    @Override
    public TradingDecision makeDecision(String symbol, double currentPrice, 
                                       Map<Long, Double> priceHistory, double balance) {
        if (priceHistory == null || priceHistory.size() < 10) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice, 
                "Недостаточно данных для анализа", 0.0);
        }
        
        // Вычисляем среднюю цену
        List<Double> prices = new ArrayList<>(priceHistory.values());
        double averagePrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(currentPrice);
        
        // Вычисляем стандартное отклонение
        double variance = prices.stream()
            .mapToDouble(p -> Math.pow(p - averagePrice, 2))
            .average()
            .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        // Определяем отклонение от среднего
        double deviation = (currentPrice - averagePrice) / averagePrice;
        
        // Вычисляем волатильность для фильтрации
        double volatility = stdDev / averagePrice;
        
        // Принимаем решение (только при достаточной уверенности)
        if (deviation < -DEVIATION_THRESHOLD && volatility < 0.05) {
            // Цена значительно ниже среднего - покупаем
            double amount = Math.min(balance * MAX_POSITION_SIZE / currentPrice, 
                                    balance / currentPrice * 0.3);
            double confidence = Math.min(0.95, 0.65 + Math.abs(deviation) * 12);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.BUY, amount, currentPrice,
                    String.format("ПОКУПКА: Цена на %.2f%% ниже среднего (среднее: %.2f, уверенность: %.1f%%)", 
                        deviation * 100, averagePrice, confidence * 100), confidence);
            }
        } else if (deviation > DEVIATION_THRESHOLD && volatility < 0.05) {
            // Цена значительно выше среднего - шортим
            double amount = balance * MAX_POSITION_SIZE / currentPrice;
            double confidence = Math.min(0.95, 0.65 + Math.abs(deviation) * 12);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.SELL, amount, currentPrice,
                    String.format("ШОРТ: Цена на %.2f%% выше среднего (среднее: %.2f, уверенность: %.1f%%)", 
                        deviation * 100, averagePrice, confidence * 100), confidence);
            }
        }
        
        // Цена близка к среднему - удерживаем
        return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
            String.format("Цена близка к среднему (отклонение: %.2f%%)", deviation * 100), 0.5);
    }
    
    @Override
    public String getName() {
        return "Mean Reversion (Возврат к среднему)";
    }
    
    @Override
    public String getDescription() {
        return "Покупает при падении цены ниже среднего, продает при росте выше среднего";
    }
}
