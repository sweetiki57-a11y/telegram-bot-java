package com.example.telegrambot.trading.strategies;

import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.TradingStrategy;
import com.example.telegrambot.trading.NewCoinScanner;
import com.example.telegrambot.trading.PriceService;
import com.example.telegrambot.trading.TokenValidator;
import java.util.Map;
import java.util.List;

/**
 * Стратегия для новых монет
 * Быстро проверяет новые листинги и входит в перспективные
 */
public class NewCoinStrategy implements TradingStrategy {
    private static final double MAX_POSITION_SIZE = 0.25; // До 25% баланса на новую монету
    private static final double MIN_CONFIDENCE = 0.65; // Минимальная уверенность 65%
    private static final double QUICK_EXIT_PROFIT = 0.05; // Быстрый выход при +5%
    private static final double TIGHT_STOP = 0.02; // Плотный стоп 2%
    
    @Override
    public TradingDecision makeDecision(String symbol, double currentPrice, 
                                       Map<Long, Double> priceHistory, double balance) {
        // 1. Быстрая проверка монеты
        NewCoinScanner.CoinAnalysis analysis = NewCoinScanner.quickAnalyze(symbol);
        
        // 2. Если это скам - не входим
        if (analysis.isScam) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
                "❌ Скам монета обнаружена: " + analysis.reason, 0.0);
        }
        
        // 3. Если не валидна - не входим
        if (!analysis.isValid) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
                "⚠️ Монета не прошла валидацию: " + analysis.reason, 0.0);
        }
        
        // 4. Проверяем потенциал и уверенность
        if (!analysis.shouldTrade()) {
            return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
                String.format("⏳ Низкий потенциал (%.1f%%) или уверенность (%.1f%%)", 
                    analysis.potential * 100, analysis.confidence * 100), 0.4);
        }
        
        // 5. Проверяем что это действительно новая монета (быстрый рост)
        if (priceHistory != null && priceHistory.size() >= 5) {
            List<Double> prices = new java.util.ArrayList<>(priceHistory.values());
            java.util.Collections.sort(prices);
            
            double oldestPrice = prices.get(0);
            double priceChange = (currentPrice - oldestPrice) / oldestPrice;
            
            // Новая монета должна показывать рост или стабильность
            if (priceChange < -0.05) {
                return new TradingDecision(TradingDecision.Action.HOLD, 0, currentPrice,
                    "📉 Монета падает, ожидание лучшего момента", 0.3);
            }
        }
        
        // 6. Входим в новую монету
        double positionSize = analysis.potential > 0.7 ? MAX_POSITION_SIZE : MAX_POSITION_SIZE * 0.7;
        double amount = Math.min(balance * positionSize / currentPrice, 
                                balance / currentPrice * 0.3);
        
        double confidence = Math.min(0.95, analysis.confidence);
        
        return new TradingDecision(TradingDecision.Action.BUY, amount, currentPrice,
            String.format("🆕 НОВАЯ МОНЕТА! Вход (потенциал: %.1f%%, уверенность: %.1f%%, ликвидность: хорошая)", 
                analysis.potential * 100, confidence * 100), confidence);
    }
    
    @Override
    public String getName() {
        return "New Coin (Новые монеты)";
    }
    
    @Override
    public String getDescription() {
        return "Быстрая проверка и торговля новыми монетами, защита от скама";
    }
}
