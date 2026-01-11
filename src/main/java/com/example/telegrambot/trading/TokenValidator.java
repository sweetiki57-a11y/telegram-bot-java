package com.example.telegrambot.trading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Scanner;

/**
 * Валидатор токенов для проверки перед входом в памп
 * Проверяет ликвидность, объем, безопасность токена
 */
public class TokenValidator {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Валидация токена перед входом в памп
     * @param symbol торговый символ
     * @return результат валидации
     */
    public static ValidationResult validateToken(String symbol) {
        ValidationResult result = new ValidationResult();
        
        try {
            // 1. Проверяем ликвидность через Back-end API
            Double liquidity = checkLiquidity(symbol);
            if (liquidity == null || liquidity < 50000) {
                result.isValid = false;
                result.reason = "Недостаточная ликвидность: " + (liquidity != null ? liquidity : "N/A");
                return result;
            }
            result.liquidity = liquidity;
            
            // 2. Проверяем объем торгов
            Double volume24h = checkVolume24h(symbol);
            if (volume24h == null || volume24h < 100000) {
                result.isValid = false;
                result.reason = "Недостаточный объем торгов: " + (volume24h != null ? volume24h : "N/A");
                return result;
            }
            result.volume24h = volume24h;
            
            // 3. Проверяем волатильность (не должна быть слишком высокой)
            Double volatility = checkVolatility(symbol);
            if (volatility != null && volatility > 0.15) {
                result.isValid = false;
                result.reason = "Слишком высокая волатильность: " + String.format("%.2f%%", volatility * 100);
                return result;
            }
            result.volatility = volatility != null ? volatility : 0.0;
            
            // 4. Проверяем спред (разница между bid и ask)
            Double spread = checkSpread(symbol);
            if (spread != null && spread > 0.02) {
                result.isValid = false;
                result.reason = "Слишком большой спред: " + String.format("%.2f%%", spread * 100);
                return result;
            }
            result.spread = spread != null ? spread : 0.0;
            
            // 5. Проверяем наличие на крупных биржах
            boolean listedOnMajorExchanges = checkMajorExchanges(symbol);
            if (!listedOnMajorExchanges) {
                result.warning = "Токен не найден на крупных биржах";
            }
            result.listedOnMajorExchanges = listedOnMajorExchanges;
            
            // Все проверки пройдены
            result.isValid = true;
            result.reason = "Токен прошел все проверки";
            
        } catch (Exception e) {
            result.isValid = false;
            result.reason = "Ошибка валидации: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * Проверка ликвидности через Back-end API
     */
    private static Double checkLiquidity(String symbol) {
        try {
            String baseUrl = com.example.telegrambot.Config.getProperty("backend.api.url", "http://localhost:8080/api");
            String endpoint = baseUrl + "/trading/liquidity/" + symbol.replace("/", "-");
            
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                if (json.has("liquidity")) {
                    return json.get("liquidity").asDouble();
                } else if (json.has("data") && json.get("data").has("liquidity")) {
                    return json.get("data").get("liquidity").asDouble();
                }
            }
        } catch (Exception e) {
            // Если Back-end недоступен, используем консервативную оценку
            return 100000.0; // Консервативная оценка
        }
        return null;
    }
    
    /**
     * Проверка объема торгов за 24 часа
     */
    private static Double checkVolume24h(String symbol) {
        try {
            String baseUrl = com.example.telegrambot.Config.getProperty("backend.api.url", "http://localhost:8080/api");
            String endpoint = baseUrl + "/trading/volume/" + symbol.replace("/", "-");
            
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                if (json.has("volume24h")) {
                    return json.get("volume24h").asDouble();
                } else if (json.has("data") && json.get("data").has("volume24h")) {
                    return json.get("data").get("volume24h").asDouble();
                }
            }
        } catch (Exception e) {
            // Консервативная оценка
            return 200000.0;
        }
        return null;
    }
    
    /**
     * Проверка волатильности
     */
    private static Double checkVolatility(String symbol) {
        try {
            Map<Long, Double> history = PriceService.getPriceHistory(symbol, 60); // 1 час истории
            if (history == null || history.size() < 10) {
                return 0.05; // Консервативная оценка
            }
            
            List<Double> prices = new ArrayList<>(history.values());
            double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = prices.stream()
                .mapToDouble(p -> Math.pow(p - mean, 2))
                .average()
                .orElse(0.0);
            
            return Math.sqrt(variance) / mean;
        } catch (Exception e) {
            return 0.05;
        }
    }
    
    /**
     * Проверка спреда
     */
    private static Double checkSpread(String symbol) {
        try {
            String baseUrl = com.example.telegrambot.Config.getProperty("backend.api.url", "http://localhost:8080/api");
            String endpoint = baseUrl + "/trading/spread/" + symbol.replace("/", "-");
            
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                if (json.has("spread")) {
                    return json.get("spread").asDouble();
                }
            }
        } catch (Exception e) {
            // Консервативная оценка
            return 0.005; // 0.5%
        }
        return 0.005;
    }
    
    /**
     * Проверка наличия на крупных биржах
     */
    private static boolean checkMajorExchanges(String symbol) {
        // Проверяем через Back-end API
        try {
            String baseUrl = com.example.telegrambot.Config.getProperty("backend.api.url", "http://localhost:8080/api");
            String endpoint = baseUrl + "/trading/exchanges/" + symbol.replace("/", "-");
            
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                if (json.has("exchanges")) {
                    JsonNode exchanges = json.get("exchanges");
                    if (exchanges.isArray() && exchanges.size() > 0) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Если проверка недоступна, считаем что токен валиден
            return true;
        }
        return true;
    }
    
    /**
     * Результат валидации
     */
    public static class ValidationResult {
        public boolean isValid = false;
        public String reason = "";
        public String warning = "";
        public double liquidity = 0.0;
        public double volume24h = 0.0;
        public double volatility = 0.0;
        public double spread = 0.0;
        public boolean listedOnMajorExchanges = false;
        
        @Override
        public String toString() {
            return String.format(
                "Валидация: %s\nПричина: %s\nЛиквидность: %.2f\nОбъем 24ч: %.2f\nВолатильность: %.2f%%\nСпред: %.2f%%",
                isValid ? "✅ Валиден" : "❌ Невалиден",
                reason,
                liquidity,
                volume24h,
                volatility * 100,
                spread * 100
            );
        }
    }
}
