package com.example.telegrambot.factory;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Фабрика для создания клавиатур
 * Реализует паттерн Factory для централизованного создания UI элементов
 */
public class KeyboardFactory {
    
    /**
     * Creates main Reply keyboard
     */
    public static ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setSelective(false);
        
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        
        // First row
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🛒 Shops");
        row1.add("💰 Exchangers");
        keyboardRows.add(row1);
        
        // Second row
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Category Search");
        row2.add("📄 Header");
        keyboardRows.add(row2);
        
        // Third row
        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏆 Top");
        row3.add("📋 Menu");
        keyboardRows.add(row3);
        
        // Fourth row - Trading and Wallet
        KeyboardRow row4 = new KeyboardRow();
        row4.add("🤖 Авто-торговля");
        row4.add("💰 Кошелек");
        keyboardRows.add(row4);
        
        // Fifth row - Personal Cabinet and Auto-Buy
        KeyboardRow row5 = new KeyboardRow();
        row5.add("👤 Личный кабинет");
        row5.add("🛒 Авто-закупка");
        keyboardRows.add(row5);
        
        keyboard.setKeyboard(keyboardRows);
        return keyboard;
    }
    
    /**
     * Creates inline keyboard for main menu
     */
    public static InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Create buttons
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createCallbackButton("🛒 Shops", "main_shops"));
        row1.add(createCallbackButton("💰 Exchangers", "main_exchangers"));
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createCallbackButton("🔍 Category Search", "main_search"));
        row2.add(createCallbackButton("📄 Header", "main_header"));
        keyboard.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createCallbackButton("🏆 Top", "main_top"));
        keyboard.add(row3);
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Creates inline keyboard for shops
     */
    public static InlineKeyboardMarkup createShopsKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Create buttons for each store
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createUrlButton("Marshmello", "https://t.me/Marshmello"));
        row1.add(createUrlButton("ZoroMD", "https://t.me/ZoroMD"));
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createUrlButton("Putin", "https://t.me/Putin"));
        row2.add(createUrlButton("BILL", "https://t.me/BILL"));
        keyboard.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createUrlButton("Fresh Direct", "https://t.me/FreshDirect"));
        row3.add(createUrlButton("Albanian Store", "https://t.me/AlbanianStore"));
        keyboard.add(row3);
        
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createUrlButton("AURORA", "https://t.me/AURORA"));
        row4.add(createUrlButton("NASA", "https://t.me/NASA"));
        keyboard.add(row4);
        
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        row5.add(createUrlButton("MONACO", "https://t.me/MONACO"));
        row5.add(createUrlButton("Bellucci", "https://t.me/Bellucci"));
        keyboard.add(row5);
        
        List<InlineKeyboardButton> row6 = new ArrayList<>();
        row6.add(createUrlButton("Mara Salvatrucha", "https://t.me/MaraSalvatrucha"));
        keyboard.add(row6);
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Creates inline keyboard for exchangers
     */
    public static InlineKeyboardMarkup createExchangersKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // List of exchangers
        String[] exchangers = {
            "BLackCatEx", "TheMatrixEx", "CryptuLMDrsrv", "CandyEXC",
            "FRN_Crypto1", "Monkeys_Crypto1", "BTCBOSSMD", "BLACKROCKEX",
            "HCHANGE1", "Trust_LTC", "LTC_MAKLER", "StichLtc",
            "CryptoCOBA", "HiroshimaExc", "PROFESOR_EX", "GoldXCHG",
            "mvp_exchange", "KryptoMahNEW", "GhostCryptoMD", "Lustig_LTC777",
            "ACHiLLES_LTC", "MIKE_LTC2", "LesbeaEX"
        };
        
        // Create buttons 2 per row
        for (int i = 0; i < exchangers.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            // First button in row
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("💱 " + exchangers[i]);
            button1.setUrl("https://t.me/" + exchangers[i]);
            row.add(button1);
            
            // Second button in row (if exists)
            if (i + 1 < exchangers.length) {
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("💱 " + exchangers[i + 1]);
                button2.setUrl("https://t.me/" + exchangers[i + 1]);
                row.add(button2);
            }
            
            keyboard.add(row);
        }
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Creates inline keyboard for category search
     */
    public static InlineKeyboardMarkup createSearchCategoriesKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Create buttons for categories
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createCallbackButton("Гаш/Шиш 🍫🥦", "search_category_гаш"));
        row1.add(createCallbackButton("Cox 🥥", "search_category_cox"));
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createCallbackButton("LSD 🍭🍄", "search_category_lsd"));
        row2.add(createCallbackButton("❄️⚡", "search_category_ice"));
        keyboard.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createCallbackButton("💊💎", "search_category_pills"));
        row3.add(createCallbackButton("🍭", "search_category_candy"));
        keyboard.add(row3);
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Creates inline keyboard for top shops (random 5)
     */
    public static InlineKeyboardMarkup createTopShopsKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // List of all shops
        String[] allShops = {
            "Marshmello", "ZoroMD", "Putin", "BILL",
            "FreshDirect", "AlbanianStore", "AURORA", "NASA",
            "MONACO", "Bellucci", "MaraSalvatrucha"
        };
        
        // Select 5 random shops
        List<String> topShops = new ArrayList<>();
        Random random = new Random();
        List<String> availableShops = new ArrayList<>(Arrays.asList(allShops));
        
        for (int i = 0; i < 5 && !availableShops.isEmpty(); i++) {
            int randomIndex = random.nextInt(availableShops.size());
            topShops.add(availableShops.remove(randomIndex));
        }
        
        // Create buttons for top-5 shops
        for (int i = 0; i < topShops.size(); i++) {
            String shop = topShops.get(i);
            String emoji = "";
            switch (i) {
                case 0: emoji = "🥇"; break;
                case 1: emoji = "🥈"; break;
                case 2: emoji = "🥉"; break;
                case 3: emoji = "4️⃣"; break;
                case 4: emoji = "5️⃣"; break;
            }
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(emoji + " " + shop);
            button.setUrl("https://t.me/" + shop);
            row.add(button);
            keyboard.add(row);
        }
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Creates inline keyboard for channels by category
     */
    public static InlineKeyboardMarkup createChannelsKeyboard(List<String> channels) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Create buttons for each channel
        for (String channel : channels) {
            // Extract channel name (until first space or @)
            String channelName = channel.split("\\s+")[0];
            if (channelName.startsWith("@")) {
                channelName = channelName.substring(1); // remove @
            }
            
            // Create link button
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("🛒 " + channelName);
            button.setUrl("https://t.me/" + channelName);
            row.add(button);
            keyboard.add(row);
        }
        
        // Back to category search button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Categories");
        backButton.setCallbackData("back_to_search");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        return markup;
    }
    
    /**
     * Creates button with URL
     */
    private static InlineKeyboardButton createUrlButton(String text, String url) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setUrl(url);
        return button;
    }
    
    /**
     * Creates button with callback data
     */
    private static InlineKeyboardButton createCallbackButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
}

