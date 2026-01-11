package com.example.telegrambot.trading;

import com.example.telegrambot.trading.BackendApiClient;
import com.example.telegrambot.trading.TradingManager;
import com.example.telegrambot.trading.Trade;
import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.PriceService;
import com.example.telegrambot.trading.TokenValidator;
import com.example.telegrambot.trading.NewCoinScanner;
import com.example.telegrambot.MyTelegramBot;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис автоматической закупки новых токенов с DEX
 * Сканирует топ новых монет и автоматически покупает их
 */
public class DexAutoBuyService {
    private static DexAutoBuyService instance;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private MyTelegramBot bot;
    private Set<String> purchasedCoins = new HashSet<>();
    private Map<String, Long> coinPurchaseTime = new HashMap<>();
    private Map<String, Double> coinWatchPrices = new HashMap<>(); // Цены для отслеживания пампов
    private Set<Long> notificationSubscribers = new HashSet<>(); // Подписчики на уведомления
    private static final long HOLD_TIME_MINUTES = 30; // Держим позицию 30 минут
    private static final double PUMP_THRESHOLD = 0.05; // 5% рост = памп, входим
    private static final double MIN_PROFIT_EXIT = 0.10; // 10% минимальная прибыль для выхода
    private static final double GOOD_PROFIT_EXIT = 0.15; // 15% хорошая прибыль - быстро выходим
    
    private DexAutoBuyService() {
        scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public static synchronized DexAutoBuyService getInstance() {
        if (instance == null) {
            instance = new DexAutoBuyService();
        }
        return instance;
    }
    
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
     * Запустить автоматическую закупку
     */
    public void start() {
        if (isRunning) {
            System.out.println("🤖 Robotic (Авто-закупка) уже запущена");
            return;
        }
        
        isRunning = true;
        System.out.println("🛒 Запуск автоматической закупки новых токенов...");
        
        // Сканируем новые монеты каждую минуту (для отслеживания пампов)
        scheduler.scheduleAtFixedRate(this::scanAndWatchNewCoins, 0, 1, TimeUnit.MINUTES);
        
        // Проверяем позиции для продажи каждые 30 секунд
        scheduler.scheduleAtFixedRate(this::checkPositionsForSale, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Остановить автоматическую закупку
     */
    public void stop() {
        if (!isRunning) {
            System.out.println("Авто-закупка не запущена");
            return;
        }
        
        isRunning = false;
        scheduler.shutdown();
        System.out.println("⏹️ Авто-закупка остановлена");
        
        scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Сканирование и отслеживание новых монет (ждем пампов перед покупкой)
     */
    private void scanAndWatchNewCoins() {
        if (!isRunning) return;
        
        try {
            System.out.println("🔍 Сканирование новых монет на DEX...");
            
            // Получаем топ новых монет
            List<NewCoinInfo> newCoins = getTopNewCoinsFromDex();
            
            if (newCoins.isEmpty()) {
                return;
            }
            
            double balance = TradingManager.getAvailableBalance(0);
            if (balance < 10) {
                return;
            }
            
            // Отслеживаем топ-5 новых монет
            int maxCoins = Math.min(5, newCoins.size());
            for (int i = 0; i < maxCoins; i++) {
                NewCoinInfo coin = newCoins.get(i);
                
                // Пропускаем если уже купили
                if (purchasedCoins.contains(coin.symbol)) {
                    continue;
                }
                
                // Валидация токена
                TokenValidator.ValidationResult validation = TokenValidator.validateToken(coin.symbol);
                if (!validation.isValid || validation.liquidity < 50000) {
                    continue;
                }
                
                // Получаем текущую цену
                Double currentPrice = PriceService.getPrice(coin.symbol);
                if (currentPrice == null || currentPrice <= 0) {
                    continue;
                }
                
                // Если монета еще не отслеживается - начинаем отслеживать
                if (!coinWatchPrices.containsKey(coin.symbol)) {
                    coinWatchPrices.put(coin.symbol, currentPrice);
                    System.out.println("👀 Начато отслеживание: " + coin.symbol + " по цене " + currentPrice);
                    continue;
                }
                
                // Проверяем рост цены (памп)
                double watchPrice = coinWatchPrices.get(coin.symbol);
                double priceChange = (currentPrice - watchPrice) / watchPrice;
                
                // Если памп >= 5% - быстро входим!
                if (priceChange >= PUMP_THRESHOLD) {
                    System.out.println("🚀 ПАМП ОБНАРУЖЕН! " + coin.symbol + " вырос на " + 
                        String.format("%.2f", priceChange * 100) + "%");
                    
                    // Быстро покупаем!
                    double positionSize = balance * 0.15; // 15% баланса при пампе
                    double amount = positionSize / currentPrice;
                    
                    TradingDecision decision = new TradingDecision(
                        TradingDecision.Action.BUY, 
                        amount, 
                        currentPrice,
                        "🚀 ПАМП! Быстрый вход в " + coin.symbol + " (рост: " + 
                        String.format("%.2f", priceChange * 100) + "%)",
                        0.95 // Очень высокая уверенность при пампе
                    );
                    
                    Trade trade = TradingManager.openTrade(coin.symbol, decision);
                    
                    if (trade != null) {
                        purchasedCoins.add(coin.symbol);
                        coinPurchaseTime.put(coin.symbol, System.currentTimeMillis());
                        coinWatchPrices.remove(coin.symbol); // Убираем из отслеживания
                        
                        System.out.println("✅ БЫСТРЫЙ ВХОД! Куплен " + coin.symbol + " на " + positionSize);
                        
                        // Уведомление о покупке
                        sendNotification("🚀 *ПАМП ОБНАРУЖЕН И КУПЛЕН!*\n\n" +
                            "💰 Символ: *" + coin.symbol + "*\n" +
                            "📈 Рост: *+" + String.format("%.2f", priceChange * 100) + "%*\n" +
                            "💵 Сумма: *" + String.format("%.2f", positionSize) + " USDT*\n" +
                            "📊 Цена входа: *" + String.format("%.8f", currentPrice) + "*\n" +
                            "⏰ Время: " + new java.util.Date().toString());
                    }
                } else {
                    // Обновляем цену отслеживания
                    coinWatchPrices.put(coin.symbol, currentPrice);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при сканировании: " + e.getMessage());
        }
    }
    
    /**
     * Проверка позиций для продажи
     */
    private void checkPositionsForSale() {
        if (!isRunning) return;
        
        try {
            for (String symbol : new ArrayList<>(purchasedCoins)) {
                Long purchaseTime = coinPurchaseTime.get(symbol);
                if (purchaseTime == null) continue;
                
                long minutesHeld = (System.currentTimeMillis() - purchaseTime) / (60 * 1000);
                
                // Проверяем прошло ли время удержания
                if (minutesHeld >= HOLD_TIME_MINUTES) {
                    Double currentPrice = PriceService.getPrice(symbol);
                    if (currentPrice == null) continue;
                    
                    // Получаем открытую позицию
                    List<Trade> openTrades = TradingManager.getOpenTrades();
                    Trade trade = null;
                    for (Trade t : openTrades) {
                        if (t.getSymbol().equals(symbol) && t.getType() == Trade.TradeType.BUY) {
                            trade = t;
                            break;
                        }
                    }
                    
                    if (trade != null) {
                        double profitPercent = ((currentPrice - trade.getEntryPrice()) / trade.getEntryPrice()) * 100;
                        
                        // Улучшенная логика выхода: хорошая прибыль, не минимальная
                        boolean shouldSell = false;
                        String sellReason = "";
                        
                        // Быстрый выход при хорошей прибыли (15%+)
                        if (profitPercent >= GOOD_PROFIT_EXIT * 100) {
                            shouldSell = true;
                            sellReason = "🎉 ОТЛИЧНАЯ ПРИБЫЛЬ!";
                        }
                        // Выход при минимальной прибыли (10%+) если прошло время
                        else if (profitPercent >= MIN_PROFIT_EXIT * 100 && minutesHeld >= 5) {
                            shouldSell = true;
                            sellReason = "✅ Хорошая прибыль";
                        }
                        // Стоп-лосс при убытке >-5%
                        else if (profitPercent <= -5.0) {
                            shouldSell = true;
                            sellReason = "⚠️ Стоп-лосс";
                        }
                        // Выход после 30 минут если есть хоть какая-то прибыль
                        else if (minutesHeld >= HOLD_TIME_MINUTES && profitPercent > 0) {
                            shouldSell = true;
                            sellReason = "⏰ Время удержания истекло";
                        }
                        
                        if (shouldSell) {
                            TradingManager.closeTrade(trade.getId(), currentPrice);
                            purchasedCoins.remove(symbol);
                            coinPurchaseTime.remove(symbol);
                            
                            System.out.println("✅ Продана позиция " + symbol + ": " + sellReason + 
                                " (прибыль: " + String.format("%.2f", profitPercent) + "%)");
                            
                            // Уведомление о продаже
                            String emoji = profitPercent >= GOOD_PROFIT_EXIT * 100 ? "🎉" : 
                                         profitPercent >= MIN_PROFIT_EXIT * 100 ? "✅" : "⚠️";
                            
                            sendNotification(emoji + " *" + sellReason + "*\n\n" +
                                "📊 Символ: *" + symbol + "*\n" +
                                "📈 Прибыль: *" + String.format("%.2f", profitPercent) + "%*\n" +
                                "💵 Цена входа: " + String.format("%.8f", trade.getEntryPrice()) + "\n" +
                                "💵 Цена выхода: " + String.format("%.8f", currentPrice) + "\n" +
                                "⏰ Время удержания: *" + minutesHeld + " минут*");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке позиций: " + e.getMessage());
        }
    }
    
    /**
     * Получение топ новых монет с DEX
     */
    private List<NewCoinInfo> getTopNewCoinsFromDex() {
        List<NewCoinInfo> coins = new ArrayList<>();
        
        try {
            // Используем NewCoinScanner для получения новых монет
            List<NewCoinScanner.NewCoin> newCoins = NewCoinScanner.scanNewCoins();
            
            for (NewCoinScanner.NewCoin coin : newCoins) {
                // Получаем быстрый анализ
                NewCoinScanner.CoinAnalysis analysis = NewCoinScanner.quickAnalyze(coin.symbol);
                
                NewCoinInfo info = new NewCoinInfo();
                info.symbol = coin.symbol;
                info.name = coin.name;
                
                // Получаем данные через валидацию
                TokenValidator.ValidationResult validation = TokenValidator.validateToken(coin.symbol);
                info.liquidity = validation.liquidity;
                // Объем получаем из валидации или используем 0
                info.volume24h = 0.0; // Можно добавить получение через API позже
                info.priceChange24h = analysis != null ? analysis.potential * 100 : 0;
                
                // Фильтруем только с хорошей ликвидностью и валидные
                if (info.liquidity > 50000 && validation.isValid && !purchasedCoins.contains(info.symbol)) {
                    coins.add(info);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения новых монет: " + e.getMessage());
        }
        
        // Сортируем по ликвидности и объему
        coins.sort((a, b) -> Double.compare(b.liquidity * b.volume24h, a.liquidity * a.volume24h));
        
        return coins;
    }
    
    /**
     * Отправка уведомления в Telegram
     */
    private void sendNotification(String message) {
        if (bot == null) {
            System.out.println("📢 Уведомление: " + message);
            return;
        }
        
        try {
            // Отправляем всем подписчикам
            if (!notificationSubscribers.isEmpty()) {
                for (Long chatId : notificationSubscribers) {
                    try {
                        bot.sendMessage(chatId, message);
                    } catch (Exception e) {
                        System.err.println("Ошибка отправки уведомления пользователю " + chatId + ": " + e.getMessage());
                    }
                }
            } else {
                // Если нет подписчиков, просто логируем
                System.out.println("📢 Уведомление: " + message);
            }
        } catch (Exception e) {
            System.err.println("Ошибка отправки уведомления: " + e.getMessage());
        }
    }
    
    /**
     * Информация о новой монете
     */
    private static class NewCoinInfo {
        String symbol;
        String name;
        double liquidity;
        double volume24h;
        double priceChange24h;
    }
}
