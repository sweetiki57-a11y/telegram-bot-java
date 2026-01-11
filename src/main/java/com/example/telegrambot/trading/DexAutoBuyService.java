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
    private static final long HOLD_TIME_MINUTES = 30; // Держим позицию 30 минут
    
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
     * Запустить автоматическую закупку
     */
    public void start() {
        if (isRunning) {
            System.out.println("Авто-закупка уже запущена");
            return;
        }
        
        isRunning = true;
        System.out.println("🛒 Запуск автоматической закупки новых токенов...");
        
        // Сканируем и покупаем каждые 3 минуты
        scheduler.scheduleAtFixedRate(this::scanAndBuyNewCoins, 0, 3, TimeUnit.MINUTES);
        
        // Проверяем позиции для продажи каждую минуту
        scheduler.scheduleAtFixedRate(this::checkPositionsForSale, 60, 60, TimeUnit.SECONDS);
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
     * Сканирование и покупка новых токенов
     */
    private void scanAndBuyNewCoins() {
        if (!isRunning) return;
        
        try {
            System.out.println("🔍 Сканирование топ новых монет на DEX...");
            
            // Получаем топ новых монет с DEX через Backend API
            List<NewCoinInfo> newCoins = getTopNewCoinsFromDex();
            
            if (newCoins.isEmpty()) {
                System.out.println("Новых монет не найдено");
                return;
            }
            
            System.out.println("✅ Найдено новых монет: " + newCoins.size());
            
            double balance = TradingManager.getAvailableBalance(0);
            if (balance < 10) {
                System.out.println("⚠️ Недостаточно баланса для покупки");
                return;
            }
            
            // Покупаем топ-3 новые монеты
            int maxCoins = Math.min(3, newCoins.size());
            for (int i = 0; i < maxCoins; i++) {
                NewCoinInfo coin = newCoins.get(i);
                
                // Проверяем что еще не купили
                if (purchasedCoins.contains(coin.symbol)) {
                    continue;
                }
                
                // Валидация токена
                TokenValidator.ValidationResult validation = TokenValidator.validateToken(coin.symbol);
                if (!validation.isValid) {
                    System.out.println("❌ Токен " + coin.symbol + " не прошел валидацию: " + validation.reason);
                    continue;
                }
                
                // Получаем цену
                Double currentPrice = PriceService.getPrice(coin.symbol);
                if (currentPrice == null || currentPrice <= 0) {
                    System.out.println("⚠️ Не удалось получить цену для " + coin.symbol);
                    continue;
                }
                
                // Вычисляем размер позиции (10% баланса на новую монету)
                double positionSize = balance * 0.10;
                double amount = positionSize / currentPrice;
                
                // Создаем решение о покупке
                TradingDecision decision = new TradingDecision(
                    TradingDecision.Action.BUY, 
                    amount, 
                    currentPrice,
                    "Авто-закупка нового токена с DEX (ликвидность: " + validation.liquidity + ")",
                    0.85 // Высокая уверенность для валидированных токенов
                );
                
                // Покупаем
                Trade trade = TradingManager.openTrade(coin.symbol, decision);
                
                if (trade != null) {
                    purchasedCoins.add(coin.symbol);
                    coinPurchaseTime.put(coin.symbol, System.currentTimeMillis());
                    
                    System.out.println("✅ Куплен новый токен: " + coin.symbol + " на сумму " + positionSize);
                    
                    // Отправляем уведомление
                    sendNotification("🛒 *Авто-закупка*\n\n" +
                        "✅ Куплен новый токен:\n" +
                        "💰 Символ: " + coin.symbol + "\n" +
                        "💵 Сумма: " + String.format("%.2f", positionSize) + " USDT\n" +
                        "📊 Цена: " + String.format("%.8f", currentPrice) + "\n" +
                        "📈 Ликвидность: " + String.format("%.0f", validation.liquidity) + "\n" +
                        "⏰ Держим позицию 30 минут");
                } else {
                    System.out.println("❌ Ошибка покупки " + coin.symbol);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при сканировании новых монет: " + e.getMessage());
            e.printStackTrace();
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
                        
                        // Продаем если прибыль >5% или убыток >-3%
                        if (profitPercent >= 5.0 || profitPercent <= -3.0) {
                            TradingManager.closeTrade(trade.getId(), currentPrice);
                            purchasedCoins.remove(symbol);
                            coinPurchaseTime.remove(symbol);
                            
                            System.out.println("✅ Продана позиция " + symbol + " с прибылью " + 
                                String.format("%.2f", profitPercent) + "%");
                            
                            // Уведомление
                            sendNotification("💰 *Авто-продажа*\n\n" +
                                "✅ Позиция закрыта:\n" +
                                "📊 Символ: " + symbol + "\n" +
                                "📈 Прибыль: " + String.format("%.2f", profitPercent) + "%\n" +
                                "⏰ Время удержания: " + minutesHeld + " минут");
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
     * Отправка уведомления
     */
    private void sendNotification(String message) {
        if (bot != null) {
            try {
                // Отправляем уведомление админу или всем активным пользователям
                // Можно добавить список chatId для отправки
                // Пока отправляем в лог и можно добавить отправку конкретным пользователям
                System.out.println("📢 Уведомление: " + message);
                
                // TODO: Добавить отправку конкретным пользователям через bot.sendMessage(chatId, message)
                // Можно хранить список подписчиков на уведомления
            } catch (Exception e) {
                System.err.println("Ошибка отправки уведомления: " + e.getMessage());
            }
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
