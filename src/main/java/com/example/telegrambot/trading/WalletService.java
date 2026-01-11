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
 * Сервис для работы с кошельком и реальными деньгами
 * Интеграция с Back-end API для пополнения и вывода средств
 */
public class WalletService {
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
     * Пополнить баланс реальными деньгами
     */
    public static DepositResult deposit(long userId, double amount, String paymentMethod) {
        try {
            String endpoint = baseUrl + "/wallet/deposit";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("amount", amount);
            requestBody.put("paymentMethod", paymentMethod); // CRYPTO, CARD, BANK_TRANSFER и т.д.
            
            JsonNode response = makePostRequest(endpoint, requestBody);
            
            if (response != null) {
                boolean success = response.has("success") ? response.get("success").asBoolean() : false;
                String transactionId = response.has("transactionId") ? response.get("transactionId").asText() : null;
                String paymentLink = response.has("paymentLink") ? response.get("paymentLink").asText() : null;
                String message = response.has("message") ? response.get("message").asText() : "Unknown error";
                double newBalance = response.has("balance") ? response.get("balance").asDouble() : 0.0;
                
                return new DepositResult(success, transactionId, paymentLink, message, newBalance);
            }
        } catch (Exception e) {
            System.err.println("Ошибка пополнения баланса: " + e.getMessage());
            return new DepositResult(false, null, null, "Ошибка: " + e.getMessage(), 0.0);
        }
        return new DepositResult(false, null, null, "Не удалось пополнить баланс", 0.0);
    }
    
    /**
     * Вывести средства
     */
    public static WithdrawResult withdraw(long userId, double amount, String withdrawalMethod, String address) {
        try {
            String endpoint = baseUrl + "/wallet/withdraw";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("amount", amount);
            requestBody.put("withdrawalMethod", withdrawalMethod); // CRYPTO, BANK и т.д.
            requestBody.put("address", address); // Адрес кошелька или банковский счет
            
            JsonNode response = makePostRequest(endpoint, requestBody);
            
            if (response != null) {
                boolean success = response.has("success") ? response.get("success").asBoolean() : false;
                String transactionId = response.has("transactionId") ? response.get("transactionId").asText() : null;
                String message = response.has("message") ? response.get("message").asText() : "Unknown error";
                double newBalance = response.has("balance") ? response.get("balance").asDouble() : 0.0;
                
                return new WithdrawResult(success, transactionId, message, newBalance);
            }
        } catch (Exception e) {
            System.err.println("Ошибка вывода средств: " + e.getMessage());
            return new WithdrawResult(false, null, "Ошибка: " + e.getMessage(), 0.0);
        }
        return new WithdrawResult(false, null, "Не удалось вывести средства", 0.0);
    }
    
    /**
     * Получить историю транзакций
     */
    public static JsonNode getTransactionHistory(long userId, int limit) {
        try {
            String endpoint = baseUrl + "/wallet/transactions?userId=" + userId + "&limit=" + limit;
            return makeGetRequest(endpoint);
        } catch (Exception e) {
            System.err.println("Ошибка получения истории транзакций: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Получить текущий баланс пользователя
     */
    public static WalletBalance getBalance(long userId) {
        try {
            String endpoint = baseUrl + "/wallet/balance?userId=" + userId;
            JsonNode response = makeGetRequest(endpoint);
            
            if (response != null) {
                JsonNode data = response.has("data") ? response.get("data") : response;
                
                WalletBalance balance = new WalletBalance();
                if (data.has("totalBalance")) balance.totalBalance = data.get("totalBalance").asDouble();
                if (data.has("availableBalance")) balance.availableBalance = data.get("availableBalance").asDouble();
                if (data.has("lockedBalance")) balance.lockedBalance = data.get("lockedBalance").asDouble();
                if (data.has("currency")) balance.currency = data.get("currency").asText("USDT");
                
                return balance;
            }
        } catch (Exception e) {
            System.err.println("Ошибка получения баланса: " + e.getMessage());
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
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("X-API-Key", apiKey);
        }
        
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
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
     * Результат пополнения
     */
    public static class DepositResult {
        public final boolean success;
        public final String transactionId;
        public final String paymentLink;
        public final String message;
        public final double newBalance;
        
        public DepositResult(boolean success, String transactionId, String paymentLink, 
                           String message, double newBalance) {
            this.success = success;
            this.transactionId = transactionId;
            this.paymentLink = paymentLink;
            this.message = message;
            this.newBalance = newBalance;
        }
    }
    
    /**
     * Результат вывода
     */
    public static class WithdrawResult {
        public final boolean success;
        public final String transactionId;
        public final String message;
        public final double newBalance;
        
        public WithdrawResult(boolean success, String transactionId, String message, double newBalance) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
            this.newBalance = newBalance;
        }
    }
    
    /**
     * Баланс кошелька
     */
    public static class WalletBalance {
        public double totalBalance = 0.0;
        public double availableBalance = 0.0;
        public double lockedBalance = 0.0;
        public String currency = "USDT";
    }
}
