package com.example.telegrambot.trading;

import com.example.telegrambot.trading.strategies.*;
import com.example.telegrambot.MyTelegramBot;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

/**
 * Автоматический торговый движок
 * Запускает торговлю по расписанию с использованием различных стратегий
 */
public class AutoTradingEngine {
    private static AutoTradingEngine instance;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private final List<TradingStrategy> strategies;
    private final List<String> symbols;
    private final Map<String, TradingStrategy> symbolStrategyMap;
    private MyTelegramBot bot; // Бот для отправки уведомлений
    private Set<Long> notificationSubscribers = new HashSet<>(); // Подписчики на уведомления
    
    private AutoTradingEngine() {
        // Инициализируем стратегии
        strategies = new ArrayList<>();
        strategies.add(new PumpDetectionStrategy()); // Стратегия обнаружения пампов (приоритет)
        strategies.add(new com.example.telegrambot.trading.strategies.NewCoinStrategy()); // Новые монеты
        strategies.add(new ProfitMaximizingStrategy()); // Новая прибыльная стратегия
        strategies.add(new SmartRiskManagementStrategy());
        strategies.add(new MeanReversionStrategy());
        strategies.add(new MomentumStrategy());
        
        // Торговые символы
        symbols = new ArrayList<>();
        symbols.add("BTC/USDT");
        symbols.add("ETH/USDT");
        symbols.add("BNB/USDT");
        symbols.add("SOL/USDT");
        
        // Маппинг символов на стратегии (приоритет стратегии пампов и новых монет)
        symbolStrategyMap = new HashMap<>();
        PumpDetectionStrategy pumpStrategy = new PumpDetectionStrategy();
        com.example.telegrambot.trading.strategies.NewCoinStrategy newCoinStrategy = 
            new com.example.telegrambot.trading.strategies.NewCoinStrategy();
        
        // Для известных монет - стратегия пампов
        symbolStrategyMap.put("BTC/USDT", pumpStrategy);
        symbolStrategyMap.put("ETH/USDT", pumpStrategy);
        symbolStrategyMap.put("BNB/USDT", pumpStrategy);
        symbolStrategyMap.put("SOL/USDT", pumpStrategy);
        
        // Для новых монет будет использоваться NewCoinStrategy автоматически
        
        scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public static synchronized AutoTradingEngine getInstance() {
        if (instance == null) {
            instance = new AutoTradingEngine();
        }
        return instance;
    }
    
    /**
     * Запустить автоматическую торговлю
     */
    public void start() {
        if (isRunning) {
            System.out.println("Автоматическая торговля уже запущена");
            return;
        }
        
        isRunning = true;
        System.out.println("🚀 Запуск автоматической торговли...");
        
        // Запускаем торговлю каждые 30 секунд
        scheduler.scheduleAtFixedRate(this::executeTradingCycle, 0, 30, TimeUnit.SECONDS);
        
        // Запускаем мониторинг открытых сделок каждую минуту
        scheduler.scheduleAtFixedRate(this::monitorOpenTrades, 60, 60, TimeUnit.SECONDS);
        
        // Запускаем сканирование новых монет каждые 2 минуты
        scheduler.scheduleAtFixedRate(this::scanNewCoins, 120, 120, TimeUnit.SECONDS);
    }
    
    /**
     * Остановить автоматическую торговлю
     */
    public void stop() {
        if (!isRunning) {
            System.out.println("Автоматическая торговля не запущена");
            return;
        }
        
        isRunning = false;
        scheduler.shutdown();
        System.out.println("⏹️ Автоматическая торговля остановлена");
        
        // Создаем новый scheduler для возможного перезапуска
        scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Выполнить один цикл торговли
     */
    private void executeTradingCycle() {
        if (!isRunning) return;
        
        try {
            for (String symbol : symbols) {
                // Получаем текущую цену
                Double currentPrice = PriceService.getPrice(symbol);
                if (currentPrice == null) {
                    System.err.println("Не удалось получить цену для " + symbol);
                    continue;
                }
                
                // Получаем историю цен
                Map<Long, Double> priceHistory = PriceService.getPriceHistory(symbol, 15);
                
                // Получаем баланс (используем системный баланс для автоматической торговли)
                // В реальной системе здесь должен быть userId пользователя
                double balance = TradingManager.getAvailableBalance(0);
                
                // Выбираем стратегию для символа
                TradingStrategy strategy = symbolStrategyMap.getOrDefault(symbol, 
                    new SmartRiskManagementStrategy());
                
                // Принимаем решение
                TradingDecision decision = strategy.makeDecision(symbol, currentPrice, 
                    priceHistory, balance);
                
                // Выполняем сделку если решение уверенное
                if (decision.shouldExecute()) {
                    Trade trade = TradingManager.openTrade(symbol, decision);
                    if (trade != null) {
                        System.out.println("✅ " + decision.getAction() + " " + symbol + 
                            " @ " + currentPrice + " (confidence: " + 
                            String.format("%.1f", decision.getConfidence() * 100) + "%)");
                        System.out.println("   Причина: " + decision.getReason());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка в торговом цикле: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Мониторинг открытых сделок - закрываем при достижении целей
     */
    private void monitorOpenTrades() {
        if (!isRunning) return;
        
        try {
            List<Trade> openTrades = TradingManager.getOpenTrades();
            
            for (Trade trade : openTrades) {
                Double currentPrice = PriceService.getPrice(trade.getSymbol());
                if (currentPrice == null) continue;
                
                // Вычисляем прибыль/убыток
                double profitPercent = 0.0;
                if (trade.getType() == Trade.TradeType.BUY) {
                    profitPercent = ((currentPrice - trade.getEntryPrice()) / trade.getEntryPrice()) * 100;
                } else {
                    // Для шортов: прибыль при падении цены
                    profitPercent = ((trade.getEntryPrice() - currentPrice) / trade.getEntryPrice()) * 100;
                }
                
                long minutesOpen = java.time.Duration.between(trade.getEntryTime(), 
                    java.time.LocalDateTime.now()).toMinutes();
                
                // Закрываем сделку если достигли целей (более агрессивные настройки)
                boolean shouldClose = false;
                String reason = "";
                
                // Проверяем является ли это пампом (быстрый рост)
                boolean isPump = detectPumpInTrade(trade, currentPrice);
                
                // Во время пампов - фиксируем прибыль медленнее для большей прибыли
                if (isPump) {
                    // При пампе фиксируем при +8% (медленнее для большей прибыли)
                    if (profitPercent >= 8.0) {
                        shouldClose = true;
                        reason = "🚀 Фиксация прибыли во время пампов (+" + String.format("%.2f", profitPercent) + "%)";
                    }
                    // Или при +12% если прошло больше 5 минут (больше прибыли)
                    else if (profitPercent >= 12.0 && minutesOpen >= 5) {
                        shouldClose = true;
                        reason = "🚀 Большая прибыль во время пампов (+" + String.format("%.2f", profitPercent) + "%)";
                    }
                } else {
                    // Обычная торговля - медленнее фиксируем для большей прибыли
                    // Тейк-профит: +8% (основной, медленнее)
                    if (profitPercent >= 8.0 && minutesOpen >= 3) {
                        shouldClose = true;
                        reason = "Тейк-профит (+" + String.format("%.2f", profitPercent) + "%)";
                    }
                    // Большая прибыль: +12% если прошло больше 5 минут
                    else if (profitPercent >= 12.0 && minutesOpen >= 5) {
                        shouldClose = true;
                        reason = "Большая прибыль (+" + String.format("%.2f", profitPercent) + "%)";
                    }
                }
                
                // Общие правила (применяются всегда)
                if (!shouldClose) {
                    // Стоп-лосс: -1.5% (плотный стоп для минимизации убытков)
                    if (profitPercent <= -1.5) {
                        shouldClose = true;
                        reason = "Стоп-лосс (" + String.format("%.2f", profitPercent) + "%)";
                    }
                    // Фиксация прибыли после 10 минут только если прибыль >5%
                    else if (profitPercent > 5.0 && minutesOpen >= 10) {
                        shouldClose = true;
                        reason = "Фиксация прибыли (+" + String.format("%.2f", profitPercent) + "%)";
                    }
                    // Защита от долгих убыточных позиций (10 минут)
                    else if (profitPercent < 0 && minutesOpen >= 10) {
                        shouldClose = true;
                        reason = "Закрытие убыточной позиции (" + String.format("%.2f", profitPercent) + "%)";
                    }
                }
                
                if (shouldClose) {
                    TradingManager.closeTrade(trade.getId(), currentPrice);
                    System.out.println("🔒 Закрыта сделка " + trade.getId() + ": " + reason);
                    
                    // Отправляем уведомление о закрытии сделки (ВСЕ сделки - и прибыльные, и убыточные)
                    sendTradeNotification(trade, currentPrice, profitPercent, reason);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при мониторинге сделок: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Сканирование новых монет и автоматический вход
     */
    private void scanNewCoins() {
        if (!isRunning) return;
        
        try {
            System.out.println("🔍 Сканирование новых монет...");
            List<com.example.telegrambot.trading.NewCoinScanner.NewCoin> newCoins = 
                com.example.telegrambot.trading.NewCoinScanner.scanNewCoins();
            
            if (newCoins.isEmpty()) {
                return;
            }
            
            System.out.println("✅ Найдено новых монет: " + newCoins.size());
            
            double balance = TradingManager.getAvailableBalance(0);
            com.example.telegrambot.trading.strategies.NewCoinStrategy strategy = 
                new com.example.telegrambot.trading.strategies.NewCoinStrategy();
            
            for (com.example.telegrambot.trading.NewCoinScanner.NewCoin coin : newCoins) {
                // Быстрый анализ
                com.example.telegrambot.trading.NewCoinScanner.CoinAnalysis analysis = 
                    com.example.telegrambot.trading.NewCoinScanner.quickAnalyze(coin.symbol);
                
                if (!analysis.shouldTrade()) {
                    System.out.println("⏭️ Пропущена " + coin.symbol + ": " + analysis.reason);
                    continue;
                }
                
                // Получаем цену и историю
                Double currentPrice = PriceService.getPrice(coin.symbol);
                if (currentPrice == null) {
                    continue;
                }
                
                Map<Long, Double> history = PriceService.getPriceHistory(coin.symbol, 15);
                
                // Принимаем решение
                TradingDecision decision = strategy.makeDecision(coin.symbol, currentPrice, history, balance);
                
                if (decision.shouldExecute()) {
                    Trade trade = TradingManager.openTrade(coin.symbol, decision);
                    if (trade != null) {
                        System.out.println("🆕 Вход в новую монету " + coin.symbol + 
                            " @ " + currentPrice + " (потенциал: " + 
                            String.format("%.1f", analysis.potential * 100) + "%)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при сканировании новых монет: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обнаружение пампов в открытой сделке
     */
    private boolean detectPumpInTrade(Trade trade, double currentPrice) {
        try {
            // Получаем историю цен за последние 10 минут
            Map<Long, Double> recentHistory = PriceService.getPriceHistory(trade.getSymbol(), 10);
            if (recentHistory == null || recentHistory.size() < 5) {
                return false;
            }
            
            List<Double> prices = new ArrayList<>(recentHistory.values());
            Collections.sort(prices);
            
            if (prices.size() < 3) {
                return false;
            }
            
            // Проверяем быстрый рост
            double oldestPrice = prices.get(0);
            double recentPrice = prices.get(prices.size() - 1);
            double priceChange = (recentPrice - oldestPrice) / oldestPrice;
            
            // Если рост > 5% за короткое время - это памп
            return priceChange >= 0.05;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Установить бота для отправки уведомлений
     */
    public void setBot(MyTelegramBot bot) {
        this.bot = bot;
    }
    
    /**
     * Добавить подписчика на уведомления
     */
    public void addNotificationSubscriber(long chatId) {
        notificationSubscribers.add(chatId);
    }
    
    /**
     * Удалить подписчика
     */
    public void removeNotificationSubscriber(long chatId) {
        notificationSubscribers.remove(chatId);
    }
    
    /**
     * Проверить статус
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Получить список стратегий
     */
    public List<TradingStrategy> getStrategies() {
        return new ArrayList<>(strategies);
    }
    
    /**
     * Получить список символов
     */
    public List<String> getSymbols() {
        return new ArrayList<>(symbols);
    }
    
    /**
     * Добавить символ для торговли
     */
    public void addSymbol(String symbol, TradingStrategy strategy) {
        if (!symbols.contains(symbol)) {
            symbols.add(symbol);
        }
        symbolStrategyMap.put(symbol, strategy);
    }
    
    /**
     * Удалить символ из торговли
     */
    public void removeSymbol(String symbol) {
        symbols.remove(symbol);
        symbolStrategyMap.remove(symbol);
    }
    
    /**
     * Отправка уведомления о закрытии сделки
     */
    private void sendTradeNotification(Trade trade, double exitPrice, double profitPercent, String reason) {
        if (bot == null || notificationSubscribers.isEmpty()) {
            return;
        }
        
        // Определяем эмодзи и статус в зависимости от прибыли/убытка
        String emoji;
        String status;
        if (profitPercent > 0) {
            emoji = profitPercent >= 10 ? "🎉" : "✅";
            status = "ПРИБЫЛЬ";
        } else {
            emoji = "❌";
            status = "УБЫТОК";
        }
        
        String message = emoji + " *СДЕЛКА ЗАКРЫТА: " + status + "*\n\n" +
            "📊 Символ: *" + trade.getSymbol() + "*\n" +
            "📈 Тип: *" + trade.getType() + "*\n" +
            "💰 Прибыль/Убыток: *" + String.format("%.2f", profitPercent) + "%*\n" +
            "💵 Цена входа: *" + String.format("%.8f", trade.getEntryPrice()) + "*\n" +
            "💵 Цена выхода: *" + String.format("%.8f", exitPrice) + "*\n" +
            "📝 Причина: *" + reason + "*\n" +
            "⏰ Время удержания: *" + 
            java.time.Duration.between(trade.getEntryTime(), java.time.LocalDateTime.now()).toMinutes() + 
            " минут*";
        
        // Отправляем всем подписчикам
        for (Long chatId : notificationSubscribers) {
            try {
                bot.sendMessage(chatId, message);
            } catch (Exception e) {
                System.err.println("Ошибка отправки уведомления пользователю " + chatId + ": " + e.getMessage());
            }
        }
    }
}
