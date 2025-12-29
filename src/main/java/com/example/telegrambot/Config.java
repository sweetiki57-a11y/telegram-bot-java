package com.example.telegrambot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Класс для загрузки конфигурации бота
 */
public class Config {
    private static final String CONFIG_FILE = "application.properties";
    private static Properties properties;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("Не удалось найти файл конфигурации: " + CONFIG_FILE);
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке конфигурации: " + e.getMessage());
        }
    }
    
    public static String getBotUsername() {
        return properties.getProperty("telegram.bot.username", "YOUR_BOT_USERNAME");
    }
    
    public static String getBotToken() {
        return properties.getProperty("telegram.bot.token", "YOUR_BOT_TOKEN");
    }
    
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
