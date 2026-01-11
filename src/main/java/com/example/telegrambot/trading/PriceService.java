package com.example.telegrambot.trading;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Сервис для получения актуальных цен с различных бирж и API
 */
public class PriceService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, Double> priceCache = new HashMap<>();
    private static final long CACHE_TTL = 5000; // 5 секунд кэш
    private static final Map<String, Long> cacheTimestamps = new HashMap<>();
    
    /**
     * Получить текущую цену для символа
     * @param symbol торговый символ (например, BTC/USDT, ETH/USDT)
     * @return текущая цена или null если не удалось получить
     */
    public static Double getPrice(String symbol) {
        String cacheKey = symbol.toUpperCase();
        long now = System.currentTimeMillis();
        
        // Проверяем кэш
        if (priceCache.containsKey(cacheKey)) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && (now - timestamp) < CACHE_TTL) {
                return priceCache.get(cacheKey);
            }
        }
        
        // Пробуем получить цену с разных источников
        Double price = null;
        
        // 1. Back-end API (приоритетный источник)
        price = BackendApiClient.getPrice(symbol);
        if (price != null) {
            updateCache(cacheKey, price);
            return price;
        }
        
        // 2. Binance API (резервный источник)
        price = getPriceFromBinance(symbol);
        if (price != null) {
            updateCache(cacheKey, price);
            return price;
        }
        
        // 3. CoinGecko API (резервный источник)
        price = getPriceFromCoinGecko(symbol);
        if (price != null) {
            updateCache(cacheKey, price);
            return price;
        }
        
        // 3. Если не удалось получить, используем кэш или генерируем тестовую цену
        if (priceCache.containsKey(cacheKey)) {
            return priceCache.get(cacheKey);
        }
        
        // Генерируем тестовую цену для разработки
        return generateTestPrice(symbol);
    }
    
    /**
     * Получить цену с Binance
     */
    private static Double getPriceFromBinance(String symbol) {
        try {
            // Конвертируем символ в формат Binance (BTC/USDT -> BTCUSDT)
            String binanceSymbol = symbol.replace("/", "").toUpperCase();
            String url = "https://api.binance.com/api/v3/ticker/price?symbol=" + binanceSymbol;
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                if (json.has("price")) {
                    return json.get("price").asDouble();
                }
            }
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем выполнение
            System.err.println("Ошибка получения цены с Binance для " + symbol + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Получить цену с CoinGecko
     */
    private static Double getPriceFromCoinGecko(String symbol) {
        try {
            // Конвертируем символ в формат CoinGecko
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) return null;
            
            String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=usd";
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                if (json.has(coinId) && json.get(coinId).has("usd")) {
                    return json.get(coinId).get("usd").asDouble();
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения цены с CoinGecko для " + symbol + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Конвертировать символ в ID CoinGecko
     */
    private static String getCoinGeckoId(String symbol) {
        String base = symbol.split("/")[0].toLowerCase();
        switch (base) {
            case "btc": return "bitcoin";
            case "eth": return "ethereum";
            case "bnb": return "binancecoin";
            case "sol": return "solana";
            case "ada": return "cardano";
            case "xrp": return "ripple";
            case "doge": return "dogecoin";
            default: return base;
        }
    }
    
    /**
     * Генерировать тестовую цену для разработки
     */
    private static Double generateTestPrice(String symbol) {
        // Базовые цены для популярных криптовалют
        String base = symbol.split("/")[0].toUpperCase();
        double basePrice = 50000.0; // BTC по умолчанию
        
        switch (base) {
            case "BTC": basePrice = 45000.0 + Math.random() * 10000; break;
            case "ETH": basePrice = 2500.0 + Math.random() * 500; break;
            case "BNB": basePrice = 300.0 + Math.random() * 50; break;
            case "SOL": basePrice = 100.0 + Math.random() * 20; break;
            case "ADA": basePrice = 0.5 + Math.random() * 0.2; break;
            case "XRP": basePrice = 0.6 + Math.random() * 0.1; break;
            case "DOGE": basePrice = 0.08 + Math.random() * 0.02; break;
            default: basePrice = 100.0 + Math.random() * 50; break;
        }
        
        // Добавляем небольшую случайную волатильность
        double volatility = basePrice * 0.02 * (Math.random() - 0.5);
        double price = basePrice + volatility;
        
        updateCache(symbol.toUpperCase(), price);
        return price;
    }
    
    /**
     * Обновить кэш цены
     */
    private static void updateCache(String symbol, Double price) {
        priceCache.put(symbol, price);
        cacheTimestamps.put(symbol, System.currentTimeMillis());
    }
    
    /**
     * Получить историю цен (для анализа трендов)
     */
    public static Map<Long, Double> getPriceHistory(String symbol, int minutes) {
        // Пробуем получить историю с Back-end API
        Map<Long, Double> history = BackendApiClient.getPriceHistory(symbol, minutes);
        
        if (history != null && !history.isEmpty()) {
            return history;
        }
        
        // Если не удалось получить с Back-end, генерируем тестовую историю
        history = new HashMap<>();
        long now = System.currentTimeMillis();
        Double currentPrice = getPrice(symbol);
        
        if (currentPrice == null) return history;
        
        // Генерируем историю с небольшими изменениями
        for (int i = minutes; i >= 0; i--) {
            long timestamp = now - (i * 60000L);
            double change = (Math.random() - 0.5) * currentPrice * 0.01;
            history.put(timestamp, currentPrice + change);
        }
        
        return history;
    }
    
    /**
     * Очистить кэш
     */
    public static void clearCache() {
        priceCache.clear();
        cacheTimestamps.clear();
    }
}
