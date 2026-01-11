package com.example.telegrambot.trading;

import com.example.telegrambot.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Scanner;

/**
 * Сканер новых монет для быстрой проверки и торговли
 * Обнаруживает новые листинги, проверяет их и быстро входит
 */
public class NewCoinScanner {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, Long> scannedCoins = new HashMap<>();
    private static final long SCAN_INTERVAL = 60000; // Сканируем каждую минуту
    
    /**
     * Сканировать новые монеты через Back-end API
     */
    public static List<NewCoin> scanNewCoins() {
        List<NewCoin> newCoins = new ArrayList<>();
        
        try {
            String baseUrl = Config.getProperty("backend.api.url", "http://localhost:8080/api");
            String endpoint = baseUrl + "/trading/new-coins";
            
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            String apiKey = Config.getProperty("backend.api.key", "");
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("X-API-Key", apiKey);
            }
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                JsonNode data = json.has("data") ? json.get("data") : json;
                
                if (data.isArray()) {
                    for (JsonNode coin : data) {
                        NewCoin newCoin = parseCoin(coin);
                        if (newCoin != null && isNewCoin(newCoin.symbol)) {
                            newCoins.add(newCoin);
                            scannedCoins.put(newCoin.symbol, System.currentTimeMillis());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка сканирования новых монет: " + e.getMessage());
        }
        
        return newCoins;
    }
    
    /**
     * Быстрая проверка монеты (скам или нет)
     */
    public static CoinAnalysis quickAnalyze(String symbol) {
        CoinAnalysis analysis = new CoinAnalysis();
        analysis.symbol = symbol;
        
        try {
            // 1. Получаем базовую информацию
            Double price = PriceService.getPrice(symbol);
            if (price == null) {
                analysis.isValid = false;
                analysis.reason = "Не удалось получить цену";
                return analysis;
            }
            analysis.currentPrice = price;
            
            // 2. Валидация токена
            TokenValidator.ValidationResult validation = TokenValidator.validateToken(symbol);
            if (!validation.isValid) {
                analysis.isValid = false;
                analysis.reason = validation.reason;
                analysis.isScam = true; // Если не прошел валидацию - вероятно скам
                return analysis;
            }
            
            // 3. Проверка на скам признаки
            analysis.isScam = detectScamSigns(symbol, validation);
            
            // 4. Оценка потенциала
            analysis.potential = calculatePotential(symbol, validation);
            
            // 5. Финальная оценка
            analysis.isValid = !analysis.isScam && validation.isValid;
            analysis.confidence = calculateConfidence(validation, analysis);
            
        } catch (Exception e) {
            analysis.isValid = false;
            analysis.reason = "Ошибка анализа: " + e.getMessage();
        }
        
        return analysis;
    }
    
    /**
     * Обнаружение признаков скама
     */
    private static boolean detectScamSigns(String symbol, TokenValidator.ValidationResult validation) {
        // Признаки скама:
        // 1. Очень низкая ликвидность
        if (validation.liquidity < 10000) {
            return true;
        }
        
        // 2. Очень низкий объем
        if (validation.volume24h < 50000) {
            return true;
        }
        
        // 3. Слишком высокая волатильность (подозрительно)
        if (validation.volatility > 0.20) {
            return true;
        }
        
        // 4. Очень большой спред (плохая ликвидность)
        if (validation.spread > 0.05) {
            return true;
        }
        
        // 5. Не на крупных биржах
        if (!validation.listedOnMajorExchanges) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Расчет потенциала монеты
     */
    private static double calculatePotential(String symbol, TokenValidator.ValidationResult validation) {
        double potential = 0.0;
        
        // Высокая ликвидность = хороший потенциал
        if (validation.liquidity > 200000) {
            potential += 0.3;
        } else if (validation.liquidity > 100000) {
            potential += 0.2;
        }
        
        // Высокий объем = активная торговля
        if (validation.volume24h > 500000) {
            potential += 0.3;
        } else if (validation.volume24h > 200000) {
            potential += 0.2;
        }
        
        // Низкая волатильность = стабильность
        if (validation.volatility < 0.05) {
            potential += 0.2;
        } else if (validation.volatility < 0.10) {
            potential += 0.1;
        }
        
        // На крупных биржах = доверие
        if (validation.listedOnMajorExchanges) {
            potential += 0.2;
        }
        
        return Math.min(1.0, potential);
    }
    
    /**
     * Расчет уверенности
     */
    private static double calculateConfidence(TokenValidator.ValidationResult validation, CoinAnalysis analysis) {
        double confidence = 0.5; // Базовая уверенность
        
        if (validation.isValid) {
            confidence += 0.2;
        }
        
        if (!analysis.isScam) {
            confidence += 0.2;
        }
        
        confidence += analysis.potential * 0.1;
        
        return Math.min(0.95, confidence);
    }
    
    /**
     * Парсинг данных монеты из JSON
     */
    private static NewCoin parseCoin(JsonNode coin) {
        try {
            NewCoin newCoin = new NewCoin();
            newCoin.symbol = coin.has("symbol") ? coin.get("symbol").asText() : null;
            newCoin.name = coin.has("name") ? coin.get("name").asText() : "";
            newCoin.listingTime = coin.has("listingTime") ? coin.get("listingTime").asLong() : System.currentTimeMillis();
            newCoin.initialPrice = coin.has("initialPrice") ? coin.get("initialPrice").asDouble() : 0.0;
            newCoin.exchange = coin.has("exchange") ? coin.get("exchange").asText() : "";
            
            if (newCoin.symbol == null) {
                return null;
            }
            
            return newCoin;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Проверка является ли монета новой
     */
    private static boolean isNewCoin(String symbol) {
        if (!scannedCoins.containsKey(symbol)) {
            return true;
        }
        
        long lastScan = scannedCoins.get(symbol);
        // Считаем новой если не сканировали больше 5 минут
        return (System.currentTimeMillis() - lastScan) > 300000;
    }
    
    /**
     * Новая монета
     */
    public static class NewCoin {
        public String symbol;
        public String name;
        public long listingTime;
        public double initialPrice;
        public String exchange;
    }
    
    /**
     * Анализ монеты
     */
    public static class CoinAnalysis {
        public String symbol;
        public boolean isValid = false;
        public boolean isScam = false;
        public double currentPrice = 0.0;
        public double potential = 0.0; // 0.0 - 1.0
        public double confidence = 0.0; // 0.0 - 1.0
        public String reason = "";
        
        public boolean shouldTrade() {
            return isValid && !isScam && confidence > 0.65 && potential > 0.5;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Анализ %s: %s\nПотенциал: %.1f%%, Уверенность: %.1f%%\n%s",
                symbol,
                isValid && !isScam ? "✅ Валидна" : (isScam ? "❌ Скам" : "⚠️ Невалидна"),
                potential * 100,
                confidence * 100,
                reason
            );
        }
    }
}
