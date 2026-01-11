package com.example.telegrambot.trading.strategies;

import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.TradingStrategy;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Умная стратегия с управлением рисками
 * Комбинирует несколько индикаторов и учитывает риски
 */
public class SmartRiskManagementStrategy implements TradingStrategy {
    private static final double MAX_POSITION_SIZE = 0.2; // Максимум 20% баланса
    private static final double MIN_CONFIDENCE = 0.7; // Минимальная уверенность 70% (повышено для большего плюса)
    private static final double STOP_LOSS_PERCENT = 0.02; // Стоп-лосс 2%
    private static final double TAKE_PROFIT_PERCENT = 0.08; // Тейк-профит 8% (медленная фиксация для большей прибыли)
    
    @Override
    public TradingDecision makeDecision(String symbol, double currentPrice, 
                                       Map<Long, Double> priceHistory, double balance) {
        if (priceHistory == null || priceHistory.size() < 15) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice, 
                "Недостаточно данных для анализа", 0.0);
        }
        
        List<Double> prices = new ArrayList<>(priceHistory.values());
        Collections.sort(prices);
        
        // 1. Вычисляем RSI (Relative Strength Index)
        double rsi = calculateRSI(prices);
        
        // 2. Вычисляем MACD
        double[] macd = calculateMACD(prices);
        
        // 3. Вычисляем волатильность
        double volatility = calculateVolatility(prices);
        
        // 4. Вычисляем тренд
        double trend = calculateTrend(prices);
        
        // 5. Комбинируем сигналы
        double buySignal = 0.0;
        double sellSignal = 0.0;
        
        // RSI сигналы (более консервативные пороги)
        if (rsi < 25) buySignal += 0.35; // Сильная перепроданность
        else if (rsi < 30) buySignal += 0.25; // Перепроданность
        if (rsi > 75) sellSignal += 0.35; // Сильная перекупленность (шорт)
        else if (rsi > 70) sellSignal += 0.25; // Перекупленность
        
        // MACD сигналы (только сильные)
        if (macd[0] > macd[1] && macd[0] > 0 && macd[1] > 0) buySignal += 0.3; // Сильный бычий кросс
        if (macd[0] < macd[1] && macd[0] < 0 && macd[1] < 0) sellSignal += 0.3; // Сильный медвежий кросс (шорт)
        
        // Тренд сигналы (только сильные тренды)
        if (trend > 0.015) buySignal += 0.25; // Сильный восходящий тренд
        if (trend < -0.015) sellSignal += 0.25; // Сильный нисходящий тренд (шорт)
        
        // Волатильность - снижаем уверенность при высокой волатильности
        double volatilityPenalty = Math.min(0.25, volatility * 8);
        
        // Принимаем решение (только при высокой уверенности)
        if (buySignal > sellSignal + 0.3 && volatility < 0.04 && rsi < 45) {
            double amount = Math.min(balance * MAX_POSITION_SIZE / currentPrice, 
                                    balance / currentPrice * 0.25);
            double confidence = Math.min(0.95, buySignal - volatilityPenalty);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.BUY, amount, currentPrice,
                    String.format("ПОКУПКА: RSI=%.1f, MACD=%.2f, тренд=%.2f%%, уверенность=%.1f%%", 
                        rsi, macd[0], trend * 100, confidence * 100), confidence);
            }
        } else if (sellSignal > buySignal + 0.3 && volatility < 0.04 && rsi > 55) {
            // Шорт: продаем при высокой цене
            double amount = balance * MAX_POSITION_SIZE / currentPrice;
            double confidence = Math.min(0.95, sellSignal - volatilityPenalty);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.SELL, amount, currentPrice,
                    String.format("ШОРТ: RSI=%.1f, MACD=%.2f, тренд=%.2f%%, уверенность=%.1f%%", 
                        rsi, macd[0], trend * 100, confidence * 100), confidence);
            }
        }
        
        // Удерживаем позицию
        return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
            String.format("Ожидание лучшего сигнала (RSI=%.1f, волатильность=%.2f%%)", 
                rsi, volatility * 100), 0.5);
    }
    
    private double calculateRSI(List<Double> prices) {
        if (prices.size() < 14) return 50.0;
        
        List<Double> recent = prices.subList(prices.size() - 14, prices.size());
        double gains = 0.0;
        double losses = 0.0;
        
        for (int i = 1; i < recent.size(); i++) {
            double change = recent.get(i) - recent.get(i - 1);
            if (change > 0) gains += change;
            else losses += Math.abs(change);
        }
        
        double avgGain = gains / 14;
        double avgLoss = losses / 14;
        
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    private double[] calculateMACD(List<Double> prices) {
        if (prices.size() < 26) return new double[]{0.0, 0.0};
        
        // EMA12
        double ema12 = calculateEMA(prices, 12);
        // EMA26
        double ema26 = calculateEMA(prices, 26);
        
        double macd = ema12 - ema26;
        double signal = macd * 0.9; // Упрощенный сигнал
        
        return new double[]{macd, signal};
    }
    
    private double calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) return prices.get(prices.size() - 1);
        
        List<Double> recent = prices.subList(prices.size() - period, prices.size());
        double multiplier = 2.0 / (period + 1);
        double ema = recent.get(0);
        
        for (int i = 1; i < recent.size(); i++) {
            ema = (recent.get(i) * multiplier) + (ema * (1 - multiplier));
        }
        
        return ema;
    }
    
    private double calculateVolatility(List<Double> prices) {
        if (prices.size() < 2) return 0.0;
        
        double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = prices.stream()
            .mapToDouble(p -> Math.pow(p - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance) / mean;
    }
    
    private double calculateTrend(List<Double> prices) {
        if (prices.size() < 2) return 0.0;
        
        double first = prices.get(0);
        double last = prices.get(prices.size() - 1);
        
        return (last - first) / first;
    }
    
    @Override
    public String getName() {
        return "Smart Risk Management (Умное управление рисками)";
    }
    
    @Override
    public String getDescription() {
        return "Комбинирует RSI, MACD и анализ волатильности для безопасной торговли";
    }
}
