package com.example.telegrambot.trading.strategies;

import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.TradingStrategy;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Агрессивная стратегия максимизации прибыли
 * Фокусируется на высоковероятных сделках с быстрым выходом
 */
public class ProfitMaximizingStrategy implements TradingStrategy {
    private static final double MAX_POSITION_SIZE = 0.15; // Максимум 15% баланса
    private static final double MIN_CONFIDENCE = 0.75; // Минимальная уверенность 75%
    private static final double QUICK_PROFIT_TARGET = 0.02; // Быстрый выход при +2%
    private static final double TIGHT_STOP_LOSS = 0.015; // Плотный стоп-лосс 1.5%
    
    @Override
    public TradingDecision makeDecision(String symbol, double currentPrice, 
                                       Map<Long, Double> priceHistory, double balance) {
        if (priceHistory == null || priceHistory.size() < 20) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice, 
                "Недостаточно данных для анализа", 0.0);
        }
        
        List<Double> prices = new ArrayList<>(priceHistory.values());
        Collections.sort(prices);
        
        // 1. Вычисляем множественные индикаторы
        double rsi = calculateRSI(prices);
        double[] macd = calculateMACD(prices);
        double volatility = calculateVolatility(prices);
        double trend = calculateTrend(prices);
        double momentum = calculateMomentum(prices);
        double support = findSupportLevel(prices);
        double resistance = findResistanceLevel(prices);
        
        // 2. Анализ паттернов
        boolean bullishPattern = detectBullishPattern(prices);
        boolean bearishPattern = detectBearishPattern(prices);
        
        // 3. Комбинируем сигналы с весами
        double buySignal = 0.0;
        double sellSignal = 0.0;
        
        // RSI сигналы (30% веса)
        if (rsi < 25) buySignal += 0.3; // Сильная перепроданность
        else if (rsi < 35) buySignal += 0.2; // Перепроданность
        if (rsi > 75) sellSignal += 0.3; // Сильная перекупленность
        else if (rsi > 65) sellSignal += 0.2; // Перекупленность
        
        // MACD сигналы (25% веса)
        if (macd[0] > macd[1] && macd[0] > 0 && macd[1] > 0) buySignal += 0.25; // Сильный бычий кросс
        else if (macd[0] > macd[1]) buySignal += 0.15; // Слабый бычий кросс
        if (macd[0] < macd[1] && macd[0] < 0 && macd[1] < 0) sellSignal += 0.25; // Сильный медвежий кросс
        else if (macd[0] < macd[1]) sellSignal += 0.15; // Слабый медвежий кросс
        
        // Тренд и момент (20% веса)
        if (trend > 0.02 && momentum > 0.01) buySignal += 0.2; // Сильный восходящий тренд
        else if (trend > 0.01) buySignal += 0.1;
        if (trend < -0.02 && momentum < -0.01) sellSignal += 0.2; // Сильный нисходящий тренд
        else if (trend < -0.01) sellSignal += 0.1;
        
        // Паттерны (15% веса)
        if (bullishPattern) buySignal += 0.15;
        if (bearishPattern) sellSignal += 0.15;
        
        // Поддержка/сопротивление (10% веса)
        double distanceToSupport = (currentPrice - support) / currentPrice;
        double distanceToResistance = (resistance - currentPrice) / currentPrice;
        if (distanceToSupport < 0.01 && distanceToSupport > -0.01) buySignal += 0.1; // Уровень поддержки
        if (distanceToResistance < 0.01 && distanceToResistance > -0.01) sellSignal += 0.1; // Уровень сопротивления
        
        // Шорты: если сильный медвежий сигнал
        boolean canShort = sellSignal > 0.6 && trend < -0.015 && rsi > 60;
        
        // Шорты: продаем при высокой цене, покупаем обратно при падении
        if (canShort && volatility < 0.04) {
            double amount = Math.min(balance * MAX_POSITION_SIZE / currentPrice, 
                                    balance / currentPrice * 0.2);
            double confidence = Math.min(0.95, sellSignal);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.SELL, amount, currentPrice,
                    String.format("ШОРТ: Сильный медвежий сигнал (RSI=%.1f, тренд=%.2f%%, уверенность=%.1f%%)", 
                        rsi, trend * 100, confidence * 100), confidence);
            }
        }
        
        // Покупка: только при очень сильном сигнале
        if (buySignal > sellSignal + 0.25 && volatility < 0.04 && rsi < 50) {
            double amount = Math.min(balance * MAX_POSITION_SIZE / currentPrice, 
                                    balance / currentPrice * 0.25);
            double confidence = Math.min(0.95, buySignal);
            
            if (confidence >= MIN_CONFIDENCE) {
                return new TradingDecision(TradingDecision.Action.BUY, amount, currentPrice,
                    String.format("ПОКУПКА: Сильный бычий сигнал (RSI=%.1f, MACD=%.2f, тренд=%.2f%%, уверенность=%.1f%%)", 
                        rsi, macd[0], trend * 100, confidence * 100), confidence);
            }
        }
        
        // Удерживаем если сигналы слабые
        return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
            String.format("Ожидание сильного сигнала (buy=%.2f, sell=%.2f, RSI=%.1f)", 
                buySignal, sellSignal, rsi), 0.5);
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
        
        double ema12 = calculateEMA(prices, 12);
        double ema26 = calculateEMA(prices, 26);
        
        double macd = ema12 - ema26;
        double signal = calculateEMA(prices.subList(prices.size() - 9, prices.size()), 9);
        
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
        if (prices.size() < 10) return 0.0;
        
        double first = prices.get(0);
        double last = prices.get(prices.size() - 1);
        
        return (last - first) / first;
    }
    
    private double calculateMomentum(List<Double> prices) {
        if (prices.size() < 5) return 0.0;
        
        double shortMA = prices.subList(prices.size() - 3, prices.size())
            .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double longMA = prices.subList(prices.size() - 10, prices.size())
            .stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return (shortMA - longMA) / longMA;
    }
    
    private double findSupportLevel(List<Double> prices) {
        if (prices.size() < 5) return prices.get(0);
        
        // Находим локальные минимумы
        List<Double> lows = new ArrayList<>();
        for (int i = 1; i < prices.size() - 1; i++) {
            if (prices.get(i) < prices.get(i - 1) && prices.get(i) < prices.get(i + 1)) {
                lows.add(prices.get(i));
            }
        }
        
        if (lows.isEmpty()) return prices.get(0);
        return lows.stream().mapToDouble(Double::doubleValue).min().orElse(prices.get(0));
    }
    
    private double findResistanceLevel(List<Double> prices) {
        if (prices.size() < 5) return prices.get(prices.size() - 1);
        
        // Находим локальные максимумы
        List<Double> highs = new ArrayList<>();
        for (int i = 1; i < prices.size() - 1; i++) {
            if (prices.get(i) > prices.get(i - 1) && prices.get(i) > prices.get(i + 1)) {
                highs.add(prices.get(i));
            }
        }
        
        if (highs.isEmpty()) return prices.get(prices.size() - 1);
        return highs.stream().mapToDouble(Double::doubleValue).max().orElse(prices.get(prices.size() - 1));
    }
    
    private boolean detectBullishPattern(List<Double> prices) {
        if (prices.size() < 5) return false;
        
        // Проверяем паттерн "двойное дно" или восходящий тренд
        double last = prices.get(prices.size() - 1);
        double prev = prices.get(prices.size() - 2);
        double prev2 = prices.get(prices.size() - 3);
        
        // Восходящий паттерн: цена растет
        return last > prev && prev > prev2;
    }
    
    private boolean detectBearishPattern(List<Double> prices) {
        if (prices.size() < 5) return false;
        
        // Проверяем паттерн "двойная вершина" или нисходящий тренд
        double last = prices.get(prices.size() - 1);
        double prev = prices.get(prices.size() - 2);
        double prev2 = prices.get(prices.size() - 3);
        
        // Нисходящий паттерн: цена падает
        return last < prev && prev < prev2;
    }
    
    @Override
    public String getName() {
        return "Profit Maximizing (Максимизация прибыли)";
    }
    
    @Override
    public String getDescription() {
        return "Агрессивная стратегия с фокусом на высоковероятные сделки, поддержка шортов";
    }
}
