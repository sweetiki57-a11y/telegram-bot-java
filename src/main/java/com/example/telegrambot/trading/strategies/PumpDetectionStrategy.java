package com.example.telegrambot.trading.strategies;

import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.TradingStrategy;
import com.example.telegrambot.trading.PriceService;
import com.example.telegrambot.trading.TokenValidator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Умная стратегия обнаружения и торговли пампов
 * Обнаруживает пампы, валидирует токен, вкладывает больше при пампе, быстро фиксирует прибыль
 */
public class PumpDetectionStrategy implements TradingStrategy {
    private static final double PUMP_THRESHOLD = 0.05; // 5% рост за короткое время = памп
    private static final double STRONG_PUMP_THRESHOLD = 0.10; // 10% = сильный памп
    private static final double MAX_POSITION_SIZE_NORMAL = 0.15; // Обычная позиция 15%
    private static final double MAX_POSITION_SIZE_PUMP = 0.35; // При пампе до 35%
    private static final double QUICK_PROFIT_TARGET = 0.08; // Выход при +8% во время пампов (медленнее для большей прибыли)
    private static final double NORMAL_PROFIT_TARGET = 0.12; // Обычный выход при +12% (больше прибыли)
    private static final double TIGHT_STOP_LOSS = 0.02; // Стоп 2%
    
    @Override
    public TradingDecision makeDecision(String symbol, double currentPrice, 
                                       Map<Long, Double> priceHistory, double balance) {
        if (priceHistory == null || priceHistory.size() < 20) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice, 
                "Недостаточно данных для анализа пампов", 0.0);
        }
        
        List<Double> prices = new ArrayList<>(priceHistory.values());
        Collections.sort(prices);
        
        // 1. Обнаружение пампов
        PumpSignal pumpSignal = detectPump(prices, currentPrice);
        
        // 2. Если обнаружен памп - валидируем токен
        if (pumpSignal.isPump && pumpSignal.strength > 0.6) {
            TokenValidator.ValidationResult validation = TokenValidator.validateToken(symbol);
            
            if (!validation.isValid) {
                // Токен не прошел валидацию - не входим
                return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
                    "❌ Памп обнаружен, но токен не прошел валидацию: " + validation.reason, 0.0);
            }
            
            // 3. Умное ожидание правильного момента входа
            if (!isGoodEntryPoint(prices, currentPrice, pumpSignal)) {
                return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
                    "⏳ Памп обнаружен, ожидание лучшего момента входа (сила: " + 
                    String.format("%.1f%%", pumpSignal.strength * 100) + ")", 0.5);
            }
            
            // 4. Входим в памп с увеличенной позицией
            double positionSize = pumpSignal.isStrongPump ? 
                MAX_POSITION_SIZE_PUMP : MAX_POSITION_SIZE_NORMAL * 1.5;
            
            double amount = Math.min(balance * positionSize / currentPrice, 
                                    balance / currentPrice * 0.4);
            
            double confidence = Math.min(0.95, 0.75 + pumpSignal.strength * 0.2);
            
            return new TradingDecision(TradingDecision.Action.BUY, amount, currentPrice,
                String.format("🚀 ПАМП ОБНАРУЖЕН! Вход в памп (рост: %.2f%%, сила: %.1f%%, ликвидность: %.0f, уверенность: %.1f%%)", 
                    pumpSignal.priceChange * 100, pumpSignal.strength * 100, 
                    validation.liquidity, confidence * 100), confidence);
        }
        
        // 5. Если нет пампов - обычная торговля или удержание
        return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
            String.format("Ожидание пампов (текущий рост: %.2f%%)", 
                pumpSignal.priceChange * 100), 0.4);
    }
    
    /**
     * Обнаружение пампов
     */
    private PumpSignal detectPump(List<Double> prices, double currentPrice) {
        PumpSignal signal = new PumpSignal();
        
        if (prices.size() < 10) {
            return signal;
        }
        
        // Анализируем последние 5-10 минут
        int shortPeriod = Math.min(5, prices.size() / 4);
        int mediumPeriod = Math.min(15, prices.size() / 2);
        
        double shortPrice = prices.get(prices.size() - shortPeriod);
        double mediumPrice = prices.get(prices.size() - mediumPeriod);
        double oldestPrice = prices.get(0);
        
        // Вычисляем изменения
        double shortChange = (currentPrice - shortPrice) / shortPrice;
        double mediumChange = (currentPrice - mediumPrice) / mediumPrice;
        double longChange = (currentPrice - oldestPrice) / oldestPrice;
        
        // Вычисляем скорость роста
        double velocity = shortChange / (shortPeriod * 60.0); // % в секунду
        
        // Определяем памп
        signal.priceChange = shortChange;
        signal.velocity = velocity;
        
        // Сильный памп: >10% за короткое время
        if (shortChange >= STRONG_PUMP_THRESHOLD && velocity > 0.0001) {
            signal.isPump = true;
            signal.isStrongPump = true;
            signal.strength = Math.min(1.0, 0.7 + (shortChange - STRONG_PUMP_THRESHOLD) * 3);
        }
        // Обычный памп: 5-10%
        else if (shortChange >= PUMP_THRESHOLD && velocity > 0.00005) {
            signal.isPump = true;
            signal.isStrongPump = false;
            signal.strength = Math.min(1.0, 0.5 + (shortChange - PUMP_THRESHOLD) * 10);
        }
        // Начало пампов (раннее обнаружение)
        else if (shortChange >= PUMP_THRESHOLD * 0.6 && velocity > 0.00003 && 
                 mediumChange > shortChange * 0.5) {
            signal.isPump = true;
            signal.isStrongPump = false;
            signal.strength = 0.4 + (shortChange / PUMP_THRESHOLD) * 0.2;
        }
        
        return signal;
    }
    
    /**
     * Умное ожидание правильного момента входа
     */
    private boolean isGoodEntryPoint(List<Double> prices, double currentPrice, PumpSignal pumpSignal) {
        if (!pumpSignal.isPump) {
            return false;
        }
        
        // 1. Проверяем что памп еще не закончился (цена не начала падать)
        if (prices.size() >= 3) {
            double recent1 = prices.get(prices.size() - 1);
            double recent2 = prices.get(prices.size() - 2);
            double recent3 = prices.get(prices.size() - 3);
            
            // Если цена начала падать - уже поздно
            if (recent1 < recent2 && recent2 < recent3) {
                return false;
            }
        }
        
        // 2. Проверяем что памп не слишком старый (не входим в конце)
        // Если рост уже большой (>15%), возможно уже поздно
        if (pumpSignal.priceChange > 0.15) {
            return false; // Слишком поздно
        }
        
        // 3. Проверяем что скорость роста еще высокая
        if (pumpSignal.velocity < 0.00003) {
            return false; // Скорость упала
        }
        
        // 4. Для сильных пампов - входим быстрее
        if (pumpSignal.isStrongPump && pumpSignal.strength > 0.8) {
            return true; // Сильный памп - входим сразу
        }
        
        // 5. Для обычных пампов - ждем подтверждения (рост > 6%)
        if (pumpSignal.priceChange >= PUMP_THRESHOLD * 1.2) {
            return true; // Достаточный рост для входа
        }
        
        return false;
    }
    
    /**
     * Сигнал пампов
     */
    private static class PumpSignal {
        boolean isPump = false;
        boolean isStrongPump = false;
        double strength = 0.0; // 0.0 - 1.0
        double priceChange = 0.0; // Изменение цены в %
        double velocity = 0.0; // Скорость роста в %/сек
    }
    
    @Override
    public String getName() {
        return "Pump Detection (Обнаружение пампов)";
    }
    
    @Override
    public String getDescription() {
        return "Умное обнаружение пампов, валидация токенов, увеличенные позиции при пампе, быстрая фиксация прибыли";
    }
}
