package com.example.telegrambot.trading;

import com.example.telegrambot.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Клиент для работы с Java Back-end API
 * Использует REST API для получения цен и выполнения торговых операций
 */
public class BackendApiClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String baseUrl;
    private static String apiKey;
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        baseUrl = Config.getProperty("backend.api.url", "http://localhost:8080/api");
        apiKey = Config.getProperty("backend.api.key", "");
    }
    
    /**
     * Получить текущую цену для символа через Back-end API
     */
    public static Double getPrice(String symbol) {
        try {
            String endpoint = baseUrl + "/trading/price/" + symbol.replace("/", "-");
            JsonNode response = makeGetRequest(endpoint);
            
            if (response != null && response.has("price")) {
                return response.get("price").asDouble();
            } else if (response != null && response.has("data") && response.get("data").has("price")) {
                return response.get("data").get("price").asDouble();
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения цены с Back-end API для " + symbol + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Получить историю цен через Back-end API
     */
    public static Map<Long, Double> getPriceHistory(String symbol, int minutes) {
        Map<Long, Double> history = new HashMap<>();
        try {
            String endpoint = baseUrl + "/trading/history/" + symbol.replace("/", "-") + "?minutes=" + minutes;
            JsonNode response = makeGetRequest(endpoint);
            
            if (response != null) {
                JsonNode data = response.has("data") ? response.get("data") : response;
                if (data.isArray()) {
                    for (JsonNode item : data) {
                        if (item.has("timestamp") && item.has("price")) {
                            long timestamp = item.get("timestamp").asLong();
                            double price = item.get("price").asDouble();
                            history.put(timestamp, price);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения истории цен с Back-end API: " + e.getMessage());
        }
        return history;
    }
    
    /**
     * Выполнить торговую операцию через Back-end API
     */
    public static TradeExecutionResult executeTrade(String symbol, String action, double amount, double price) {
        try {
            String endpoint = baseUrl + "/trading/execute";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("symbol", symbol);
            requestBody.put("action", action); // BUY или SELL
            requestBody.put("amount", amount);
            requestBody.put("price", price);
            
            JsonNode response = makePostRequest(endpoint, requestBody);
            
            if (response != null) {
                boolean success = response.has("success") ? response.get("success").asBoolean() : false;
                String tradeId = response.has("tradeId") ? response.get("tradeId").asText() : null;
                String message = response.has("message") ? response.get("message").asText() : "Unknown error";
                
                return new TradeExecutionResult(success, tradeId, message);
            }
        } catch (Exception e) {
            System.err.println("Ошибка выполнения сделки через Back-end API: " + e.getMessage());
            return new TradeExecutionResult(false, null, "Ошибка: " + e.getMessage());
        }
        return new TradeExecutionResult(false, null, "Не удалось выполнить сделку");
    }
    
    /**
     * Получить баланс через Back-end API
     */
    public static Double getBalance() {
        try {
            String endpoint = baseUrl + "/trading/balance";
            JsonNode response = makeGetRequest(endpoint);
            
            if (response != null) {
                if (response.has("balance")) {
                    return response.get("balance").asDouble();
                } else if (response.has("data") && response.get("data").has("balance")) {
                    return response.get("data").get("balance").asDouble();
                } else if (response.has("availableBalance")) {
                    return response.get("availableBalance").asDouble();
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения баланса с Back-end API: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Получить статистику торговли через Back-end API
     */
    public static TradingStatsResponse getTradingStats() {
        try {
            String endpoint = baseUrl + "/trading/stats";
            JsonNode response = makeGetRequest(endpoint);
            
            if (response != null) {
                JsonNode data = response.has("data") ? response.get("data") : response;
                
                TradingStatsResponse stats = new TradingStatsResponse();
                if (data.has("totalTrades")) stats.totalTrades = data.get("totalTrades").asInt();
                if (data.has("profitableTrades")) stats.profitableTrades = data.get("profitableTrades").asInt();
                if (data.has("losingTrades")) stats.losingTrades = data.get("losingTrades").asInt();
                if (data.has("totalProfit")) stats.totalProfit = data.get("totalProfit").asDouble();
                if (data.has("avgProfit")) stats.avgProfit = data.get("avgProfit").asDouble();
                if (data.has("winRate")) stats.winRate = data.get("winRate").asDouble();
                if (data.has("balance")) stats.balance = data.get("balance").asDouble();
                if (data.has("availableBalance")) stats.availableBalance = data.get("availableBalance").asDouble();
                
                return stats;
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения статистики с Back-end API: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Получить список открытых сделок через Back-end API
     */
    public static JsonNode getOpenTrades() {
        try {
            String endpoint = baseUrl + "/trading/trades/open";
            return makeGetRequest(endpoint);
        } catch (Exception e) {
            System.err.println("Ошибка получения открытых сделок с Back-end API: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Выполнить GET запрос
     */
    private static JsonNode makeGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        // Добавляем API ключ если есть
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("X-API-Key", apiKey);
        }
        
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        if (conn.getResponseCode() == 200) {
            InputStream inputStream = conn.getInputStream();
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();
            inputStream.close();
            
            return objectMapper.readTree(response);
        } else {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8.name());
                String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
                System.err.println("Back-end API error (" + conn.getResponseCode() + "): " + errorResponse);
            }
        }
        return null;
    }
    
    /**
     * Выполнить POST запрос
     */
    private static JsonNode makePostRequest(String urlString, Map<String, Object> body) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        
        // Добавляем API ключ если есть
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("X-API-Key", apiKey);
        }
        
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        // Отправляем тело запроса
        String jsonBody = objectMapper.writeValueAsString(body);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        if (conn.getResponseCode() == 200 || conn.getResponseCode() == 201) {
            InputStream inputStream = conn.getInputStream();
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();
            inputStream.close();
            
            return objectMapper.readTree(response);
        } else {
            InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8.name());
                String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
                System.err.println("Back-end API error (" + conn.getResponseCode() + "): " + errorResponse);
            }
        }
        return null;
    }
    
    /**
     * Результат выполнения торговой операции
     */
    public static class TradeExecutionResult {
        public final boolean success;
        public final String tradeId;
        public final String message;
        
        public TradeExecutionResult(boolean success, String tradeId, String message) {
            this.success = success;
            this.tradeId = tradeId;
            this.message = message;
        }
    }
    
    /**
     * Статистика торговли с Back-end
     */
    public static class TradingStatsResponse {
        public int totalTrades = 0;
        public int profitableTrades = 0;
        public int losingTrades = 0;
        public double totalProfit = 0.0;
        public double avgProfit = 0.0;
        public double winRate = 0.0;
        public double balance = 0.0;
        public double availableBalance = 0.0;
    }
}
