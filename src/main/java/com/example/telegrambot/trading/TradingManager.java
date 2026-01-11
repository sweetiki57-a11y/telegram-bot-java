package com.example.telegrambot.trading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Менеджер для управления торговыми сделками
 */
public class TradingManager {
    private static final Map<String, Trade> openTrades = new ConcurrentHashMap<>();
    private static final Map<String, Trade> closedTrades = new ConcurrentHashMap<>();
    private static final Map<String, List<Trade>> tradesBySymbol = new ConcurrentHashMap<>();
    
    private static double totalBalance = 10000.0; // Начальный баланс
    private static double availableBalance = totalBalance;
    private static final double MIN_TRADE_AMOUNT = 10.0; // Минимальная сумма сделки
    
    /**
     * Открыть новую сделку через Back-end API
     */
    public static Trade openTrade(String symbol, TradingDecision decision) {
        if (decision.getAction() == TradingDecision.Action.HOLD) {
            return null;
        }
        
        // Получаем баланс с Back-end
        Double backendBalance = BackendApiClient.getBalance();
        if (backendBalance != null) {
            availableBalance = backendBalance;
            totalBalance = backendBalance;
        }
        
        // Проверяем баланс
        double requiredAmount = decision.getAmount() * decision.getPrice();
        if (requiredAmount > availableBalance && decision.getAction() == TradingDecision.Action.BUY) {
            System.err.println("Недостаточно средств для покупки: требуется " + 
                requiredAmount + ", доступно " + availableBalance);
            return null;
        }
        
        // Выполняем сделку через Back-end API
        String action = decision.getAction() == TradingDecision.Action.BUY ? "BUY" : "SELL";
        BackendApiClient.TradeExecutionResult result = BackendApiClient.executeTrade(
            symbol, action, decision.getAmount(), decision.getPrice()
        );
        
        if (!result.success) {
            System.err.println("Ошибка выполнения сделки через Back-end: " + result.message);
            return null;
        }
        
        // Создаем локальную запись о сделке
        String tradeId = result.tradeId != null ? result.tradeId : 
            "TRADE_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        
        Trade.TradeType tradeType = decision.getAction() == TradingDecision.Action.BUY ? 
            Trade.TradeType.BUY : Trade.TradeType.SELL;
        
        Trade trade = new Trade(tradeId, symbol, tradeType, decision.getAmount(), 
                               decision.getPrice(), "AutoTrading");
        
        // Сохраняем сделку локально для отображения
        openTrades.put(tradeId, trade);
        tradesBySymbol.computeIfAbsent(symbol, k -> new ArrayList<>()).add(trade);
        
        // Обновляем баланс с Back-end
        backendBalance = BackendApiClient.getBalance();
        if (backendBalance != null) {
            availableBalance = backendBalance;
        } else {
            // Локальное обновление если Back-end недоступен
            if (tradeType == Trade.TradeType.BUY) {
                availableBalance -= requiredAmount;
            } else {
                availableBalance += requiredAmount;
            }
        }
        
        return trade;
    }
    
    /**
     * Закрыть сделку
     */
    public static boolean closeTrade(String tradeId, double exitPrice) {
        Trade trade = openTrades.remove(tradeId);
        if (trade == null) {
            return false;
        }
        
        trade.close(exitPrice);
        closedTrades.put(tradeId, trade);
        
        // Обновляем баланс
        double tradeValue = trade.getCurrentValue(exitPrice);
        if (trade.getType() == Trade.TradeType.BUY) {
            availableBalance += tradeValue;
        }
        
        // Обновляем общий баланс
        totalBalance = availableBalance + getOpenTradesValue();
        
        return true;
    }
    
    /**
     * Получить текущую стоимость открытых сделок
     */
    private static double getOpenTradesValue() {
        double value = 0.0;
        for (Trade trade : openTrades.values()) {
            if (trade.getType() == Trade.TradeType.BUY) {
                // Получаем текущую цену для оценки
                Double currentPrice = PriceService.getPrice(trade.getSymbol());
                if (currentPrice != null) {
                    value += trade.getCurrentValue(currentPrice);
                } else {
                    value += trade.getAmount() * trade.getEntryPrice();
                }
            }
        }
        return value;
    }
    
    /**
     * Получить все открытые сделки
     */
    public static List<Trade> getOpenTrades() {
        return new ArrayList<>(openTrades.values());
    }
    
    /**
     * Получить все закрытые сделки
     */
    public static List<Trade> getClosedTrades() {
        return new ArrayList<>(closedTrades.values());
    }
    
    /**
     * Получить сделки по символу
     */
    public static List<Trade> getTradesBySymbol(String symbol) {
        return tradesBySymbol.getOrDefault(symbol, new ArrayList<>());
    }
    
    /**
     * Получить статистику торговли (с приоритетом Back-end API)
     */
    public static TradingStats getStats() {
        // Пробуем получить статистику с Back-end API
        BackendApiClient.TradingStatsResponse backendStats = BackendApiClient.getTradingStats();
        
        if (backendStats != null) {
            // Обновляем локальный баланс
            totalBalance = backendStats.balance;
            availableBalance = backendStats.availableBalance;
            
            return new TradingStats(
                backendStats.totalTrades,
                backendStats.profitableTrades,
                backendStats.losingTrades,
                backendStats.totalProfit,
                backendStats.avgProfit,
                backendStats.winRate,
                backendStats.balance,
                backendStats.availableBalance
            );
        }
        
        // Если Back-end недоступен, используем локальную статистику
        List<Trade> closed = getClosedTrades();
        
        int totalTrades = closed.size();
        int profitableTrades = (int) closed.stream()
            .filter(t -> t.getProfit() > 0)
            .count();
        int losingTrades = totalTrades - profitableTrades;
        
        double totalProfit = closed.stream()
            .mapToDouble(Trade::getProfit)
            .sum();
        
        double avgProfit = totalTrades > 0 ? totalProfit / totalTrades : 0.0;
        
        double winRate = totalTrades > 0 ? (double) profitableTrades / totalTrades * 100 : 0.0;
        
        return new TradingStats(totalTrades, profitableTrades, losingTrades, 
                              totalProfit, avgProfit, winRate, totalBalance, availableBalance);
    }
    
    /**
     * Получить текущий баланс (с Back-end API или WalletService)
     */
    public static double getBalance() {
        // Пробуем получить баланс через WalletService (приоритет)
        WalletService.WalletBalance walletBalance = WalletService.getBalance(0); // 0 = системный баланс
        if (walletBalance != null) {
            totalBalance = walletBalance.totalBalance;
            return totalBalance;
        }
        
        // Резерв: через BackendApiClient
        Double backendBalance = BackendApiClient.getBalance();
        if (backendBalance != null) {
            totalBalance = backendBalance;
            return backendBalance;
        }
        return totalBalance;
    }
    
    /**
     * Получить доступный баланс для пользователя (реальные деньги)
     */
    public static double getAvailableBalance(long userId) {
        // Получаем реальный баланс из кошелька
        WalletService.WalletBalance walletBalance = WalletService.getBalance(userId);
        if (walletBalance != null) {
            availableBalance = walletBalance.availableBalance;
            return availableBalance;
        }
        
        // Резерв: через BackendApiClient
        Double backendBalance = BackendApiClient.getBalance();
        if (backendBalance != null) {
            availableBalance = backendBalance;
            return backendBalance;
        }
        return availableBalance;
    }
    
    /**
     * Получить доступный баланс (системный, для обратной совместимости)
     */
    public static double getAvailableBalance() {
        return getAvailableBalance(0);
    }
    
    /**
     * Установить баланс (для инициализации)
     */
    public static void setBalance(double balance) {
        totalBalance = balance;
        availableBalance = balance;
    }
    
    /**
     * Очистить все сделки (для тестирования)
     */
    public static void clearAllTrades() {
        openTrades.clear();
        closedTrades.clear();
        tradesBySymbol.clear();
    }
    
    /**
     * Статистика торговли
     */
    public static class TradingStats {
        private final int totalTrades;
        private final int profitableTrades;
        private final int losingTrades;
        private final double totalProfit;
        private final double avgProfit;
        private final double winRate;
        private final double totalBalance;
        private final double availableBalance;
        
        public TradingStats(int totalTrades, int profitableTrades, int losingTrades,
                          double totalProfit, double avgProfit, double winRate,
                          double totalBalance, double availableBalance) {
            this.totalTrades = totalTrades;
            this.profitableTrades = profitableTrades;
            this.losingTrades = losingTrades;
            this.totalProfit = totalProfit;
            this.avgProfit = avgProfit;
            this.winRate = winRate;
            this.totalBalance = totalBalance;
            this.availableBalance = availableBalance;
        }
        
        public int getTotalTrades() { return totalTrades; }
        public int getProfitableTrades() { return profitableTrades; }
        public int getLosingTrades() { return losingTrades; }
        public double getTotalProfit() { return totalProfit; }
        public double getAvgProfit() { return avgProfit; }
        public double getWinRate() { return winRate; }
        public double getTotalBalance() { return totalBalance; }
        public double getAvailableBalance() { return availableBalance; }
        
        @Override
        public String toString() {
            return String.format(
                "Статистика торговли:\n" +
                "Всего сделок: %d\n" +
                "Прибыльных: %d (%.1f%%)\n" +
                "Убыточных: %d\n" +
                "Общая прибыль: %.2f%%\n" +
                "Средняя прибыль: %.2f%%\n" +
                "Общий баланс: %.2f\n" +
                "Доступно: %.2f",
                totalTrades, profitableTrades, winRate, losingTrades,
                totalProfit, avgProfit, totalBalance, availableBalance
            );
        }
    }
}
