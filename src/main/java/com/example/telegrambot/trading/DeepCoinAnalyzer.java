package com.example.telegrambot.trading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Глубокий анализ токенов через CoinGecko и DEXScreener
 */
public class DeepCoinAnalyzer {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Глубокий анализ токена
     */
    public static DeepAnalysis analyzeToken(String symbol) {
        DeepAnalysis analysis = new DeepAnalysis();
        analysis.symbol = symbol;
        
        try {
            // Анализ через CoinGecko
            CoinGeckoData coingeckoData = fetchFromCoinGecko(symbol);
            if (coingeckoData != null) {
                analysis.marketCap = coingeckoData.marketCap;
                analysis.volume24h = coingeckoData.volume24h;
                analysis.priceChange24h = coingeckoData.priceChange24h;
                analysis.priceChange7d = coingeckoData.priceChange7d;
                analysis.currentPrice = coingeckoData.currentPrice;
            }
            
            // Анализ через DEXScreener
            DexScreenerData dexscreenerData = fetchFromDexScreener(symbol);
            if (dexscreenerData != null) {
                analysis.liquidity = dexscreenerData.liquidity;
                analysis.pairCreatedAt = dexscreenerData.pairCreatedAt;
                analysis.fdv = dexscreenerData.fdv;
                analysis.pairAddress = dexscreenerData.pairAddress;
            }
            
            // Комплексная оценка
            analysis.score = calculateScore(analysis);
            analysis.isPromising = analysis.score >= 70;
            analysis.recommendation = generateRecommendation(analysis);
            
        } catch (Exception e) {
            analysis.error = "Ошибка анализа: " + e.getMessage();
        }
        
        return analysis;
    }
    
    /**
     * Получить данные с CoinGecko
     */
    private static CoinGeckoData fetchFromCoinGecko(String symbol) {
        try {
            String coinId = getCoinGeckoId(symbol);
            if (coinId == null) return null;
            
            String url = "https://api.coingecko.com/api/v3/coins/" + coinId + 
                "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false";
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                JsonNode marketData = json.get("market_data");
                
                if (marketData != null) {
                    CoinGeckoData data = new CoinGeckoData();
                    data.currentPrice = marketData.has("current_price") && marketData.get("current_price").has("usd") 
                        ? marketData.get("current_price").get("usd").asDouble() : 0.0;
                    data.marketCap = marketData.has("market_cap") && marketData.get("market_cap").has("usd")
                        ? marketData.get("market_cap").get("usd").asDouble() : 0.0;
                    data.volume24h = marketData.has("total_volume") && marketData.get("total_volume").has("usd")
                        ? marketData.get("total_volume").get("usd").asDouble() : 0.0;
                    
                    if (marketData.has("price_change_percentage_24h")) {
                        data.priceChange24h = marketData.get("price_change_percentage_24h").asDouble();
                    }
                    if (marketData.has("price_change_percentage_7d")) {
                        data.priceChange7d = marketData.get("price_change_percentage_7d").asDouble();
                    }
                    
                    return data;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка CoinGecko для " + symbol + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Получить данные с DEXScreener
     */
    private static DexScreenerData fetchFromDexScreener(String symbol) {
        try {
            // DEXScreener API для поиска пары
            String url = "https://api.dexscreener.com/latest/dex/search?q=" + symbol.toUpperCase();
            
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(conn.getInputStream());
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();
                
                JsonNode json = objectMapper.readTree(response);
                JsonNode pairs = json.get("pairs");
                
                if (pairs != null && pairs.isArray() && pairs.size() > 0) {
                    // Берем первую пару (обычно самая ликвидная)
                    JsonNode pair = pairs.get(0);
                    
                    DexScreenerData data = new DexScreenerData();
                    data.liquidity = pair.has("liquidity") && pair.get("liquidity").has("usd")
                        ? pair.get("liquidity").get("usd").asDouble() : 0.0;
                    data.fdv = pair.has("fdv") ? pair.get("fdv").asDouble() : 0.0;
                    data.pairAddress = pair.has("pairAddress") ? pair.get("pairAddress").asText() : "";
                    data.pairCreatedAt = pair.has("pairCreatedAt") ? pair.get("pairCreatedAt").asLong() : 0;
                    
                    return data;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка DEXScreener для " + symbol + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Конвертировать символ в ID CoinGecko
     */
    private static String getCoinGeckoId(String symbol) {
        String base = symbol.split("/")[0].toLowerCase();
        Map<String, String> mapping = new HashMap<>();
        mapping.put("btc", "bitcoin");
        mapping.put("eth", "ethereum");
        mapping.put("bnb", "binancecoin");
        mapping.put("sol", "solana");
        mapping.put("ada", "cardano");
        mapping.put("xrp", "ripple");
        mapping.put("doge", "dogecoin");
        mapping.put("matic", "matic-network");
        mapping.put("dot", "polkadot");
        mapping.put("avax", "avalanche-2");
        return mapping.getOrDefault(base, base);
    }
    
    /**
     * Расчет общего скора
     */
    private static double calculateScore(DeepAnalysis analysis) {
        double score = 0.0;
        
        // Ликвидность (макс 25 баллов)
        if (analysis.liquidity > 1000000) score += 25;
        else if (analysis.liquidity > 500000) score += 20;
        else if (analysis.liquidity > 200000) score += 15;
        else if (analysis.liquidity > 100000) score += 10;
        
        // Объем 24ч (макс 25 баллов)
        if (analysis.volume24h > 5000000) score += 25;
        else if (analysis.volume24h > 2000000) score += 20;
        else if (analysis.volume24h > 1000000) score += 15;
        else if (analysis.volume24h > 500000) score += 10;
        
        // Рост цены 24ч (макс 20 баллов)
        if (analysis.priceChange24h > 20) score += 20;
        else if (analysis.priceChange24h > 10) score += 15;
        else if (analysis.priceChange24h > 5) score += 10;
        else if (analysis.priceChange24h > 0) score += 5;
        
        // Рост цены 7д (макс 15 баллов)
        if (analysis.priceChange7d > 50) score += 15;
        else if (analysis.priceChange7d > 30) score += 10;
        else if (analysis.priceChange7d > 15) score += 5;
        
        // Market Cap (макс 15 баллов)
        if (analysis.marketCap > 100000000) score += 15;
        else if (analysis.marketCap > 50000000) score += 10;
        else if (analysis.marketCap > 10000000) score += 5;
        
        return Math.min(100, score);
    }
    
    /**
     * Генерация рекомендации
     */
    private static String generateRecommendation(DeepAnalysis analysis) {
        if (analysis.score >= 80) {
            return "🚀 ОТЛИЧНАЯ возможность! Высокий потенциал роста";
        } else if (analysis.score >= 70) {
            return "✅ Хорошая перспектива, стоит рассмотреть";
        } else if (analysis.score >= 60) {
            return "⚠️ Средний потенциал, осторожно";
        } else {
            return "❌ Низкий потенциал, не рекомендуется";
        }
    }
    
    /**
     * Данные CoinGecko
     */
    private static class CoinGeckoData {
        double currentPrice;
        double marketCap;
        double volume24h;
        double priceChange24h;
        double priceChange7d;
    }
    
    /**
     * Данные DEXScreener
     */
    private static class DexScreenerData {
        double liquidity;
        double fdv;
        String pairAddress;
        long pairCreatedAt;
    }
    
    /**
     * Результат глубокого анализа
     */
    public static class DeepAnalysis {
        public String symbol;
        public double currentPrice;
        public double marketCap;
        public double volume24h;
        public double liquidity;
        public double priceChange24h;
        public double priceChange7d;
        public double fdv;
        public String pairAddress;
        public long pairCreatedAt;
        public double score;
        public boolean isPromising;
        public String recommendation;
        public String error;
        
        @Override
        public String toString() {
            if (error != null) {
                return "❌ " + error;
            }
            
            return String.format(
                "📊 *%s*\n\n" +
                "💰 Цена: $%.8f\n" +
                "📈 Изменение 24ч: %.2f%%\n" +
                "📊 Изменение 7д: %.2f%%\n" +
                "💵 Market Cap: $%.2f\n" +
                "💧 Ликвидность: $%.2f\n" +
                "📊 Объем 24ч: $%.2f\n" +
                "⭐ Скор: %.0f/100\n" +
                "🎯 %s",
                symbol,
                currentPrice,
                priceChange24h,
                priceChange7d,
                marketCap,
                liquidity,
                volume24h,
                score,
                recommendation
            );
        }
    }
}
