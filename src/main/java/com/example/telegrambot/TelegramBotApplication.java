package com.example.telegrambot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Main application class to start the Telegram bot
 */
public class TelegramBotApplication {
    
    public static void main(String[] args) {
        try {
            // Initialize Telegram Bots API
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            
            // Register our bot
            botsApi.registerBot(new MyTelegramBot());
            
            System.out.println("Telegram bot started successfully!");
            
        } catch (TelegramApiException e) {
            System.err.println("Error starting Telegram bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
