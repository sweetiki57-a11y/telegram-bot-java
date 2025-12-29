package com.example.telegrambot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

/**
 * Class for admin panel product management
 */
public class AdminPanel {
    
    // List of admins (in real application better to store in database)
    private static final Set<Long> ADMIN_IDS = new HashSet<>();
    
    static {
        // Add administrator IDs here
        // ADMIN_IDS.add(123456789L);
    }
    
    /**
     * Check if user is administrator
     */
    public static boolean isAdmin(long userId) {
        return ADMIN_IDS.contains(userId);
    }
    
    /**
     * Add administrator
     */
    public static void addAdmin(long userId) {
        ADMIN_IDS.add(userId);
    }
    
    /**
     * Remove administrator
     */
    public static void removeAdmin(long userId) {
        ADMIN_IDS.remove(userId);
    }
    
    /**
     * Show admin panel
     */
    public static void showAdminPanel(MyTelegramBot bot, long chatId) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ You don't have administrator rights.");
            return;
        }
        
        String adminText = "🔧 Admin Panel\n\n" +
                "Select an action:";
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Product management buttons
        List<InlineKeyboardButton> productsRow = new ArrayList<>();
        InlineKeyboardButton addProductButton = new InlineKeyboardButton();
        addProductButton.setText("➕ Add Product");
        addProductButton.setCallbackData("admin_add_product");
        productsRow.add(addProductButton);
        
        InlineKeyboardButton listProductsButton = new InlineKeyboardButton();
        listProductsButton.setText("📋 Product List");
        listProductsButton.setCallbackData("admin_list_products");
        productsRow.add(listProductsButton);
        keyboard.add(productsRow);
        
        // Category management buttons
        List<InlineKeyboardButton> categoriesRow = new ArrayList<>();
        InlineKeyboardButton addCategoryButton = new InlineKeyboardButton();
        addCategoryButton.setText("📁 Add Category");
        addCategoryButton.setCallbackData("admin_add_category");
        categoriesRow.add(addCategoryButton);
        
        InlineKeyboardButton listCategoriesButton = new InlineKeyboardButton();
        listCategoriesButton.setText("📂 Category List");
        listCategoriesButton.setCallbackData("admin_list_categories");
        categoriesRow.add(listCategoriesButton);
        keyboard.add(categoriesRow);
        
        // Statistics buttons
        List<InlineKeyboardButton> statsRow = new ArrayList<>();
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("📊 Statistics");
        statsButton.setCallbackData("admin_stats");
        statsRow.add(statsButton);
        
        InlineKeyboardButton ordersButton = new InlineKeyboardButton();
        ordersButton.setText("📦 Orders");
        ordersButton.setCallbackData("admin_orders");
        statsRow.add(ordersButton);
        keyboard.add(statsRow);
        
        // Product refresh button
        List<InlineKeyboardButton> updateRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText("🔄 Refresh Products");
        updateButton.setCallbackData("admin_refresh_products");
        updateRow.add(updateButton);
        keyboard.add(updateRow);
        
        // Back button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Menu");
        backButton.setCallbackData("back_to_main_menu");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(adminText);
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show list of all products
     */
    public static void showProductsList(MyTelegramBot bot, long chatId, Map<String, List<Product>> categories) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ You don't have administrator rights.");
            return;
        }
        
        StringBuilder productsText = new StringBuilder("📋 List of All Products:\n\n");
        
        for (Map.Entry<String, List<Product>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<Product> products = entry.getValue();
            
            productsText.append("📁 ").append(category).append(":\n");
            
            for (Product product : products) {
                productsText.append("• ").append(product.getName())
                        .append(" (ID: ").append(product.getId()).append(")")
                        .append(" - ").append(product.getPrice()).append("₽")
                        .append(" [Stock: ").append(product.getStock()).append("]\n");
            }
            productsText.append("\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to admin panel button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Admin Panel");
        backButton.setCallbackData("admin_panel");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(productsText.toString());
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show list of categories
     */
    public static void showCategoriesList(MyTelegramBot bot, long chatId, Map<String, List<Product>> categories) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ You don't have administrator rights.");
            return;
        }
        
        StringBuilder categoriesText = new StringBuilder("📂 Category List:\n\n");
        
        for (Map.Entry<String, List<Product>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<Product> products = entry.getValue();
            
            categoriesText.append("📁 ").append(category)
                    .append(" (").append(products.size()).append(" products)\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to admin panel button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Admin Panel");
        backButton.setCallbackData("admin_panel");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(categoriesText.toString());
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show statistics
     */
    public static void showStats(MyTelegramBot bot, long chatId, Map<String, List<Product>> categories) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ You don't have administrator rights.");
            return;
        }
        
        int totalProducts = 0;
        int totalStock = 0;
        double totalValue = 0;
        
        for (List<Product> products : categories.values()) {
            totalProducts += products.size();
            for (Product product : products) {
                totalStock += product.getStock();
                totalValue += product.getPrice() * product.getStock();
            }
        }
        
        String statsText = "📊 Store Statistics:\n\n" +
                "📦 Total Products: " + totalProducts + "\n" +
                "📋 Total Categories: " + categories.size() + "\n" +
                "📦 Total Stock: " + totalStock + " pcs.\n" +
                "💰 Total Value: " + String.format("%.2f", totalValue) + "₽\n\n" +
                "📁 By Categories:\n";
        
        for (Map.Entry<String, List<Product>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<Product> products = entry.getValue();
            int categoryStock = products.stream().mapToInt(Product::getStock).sum();
            double categoryValue = products.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
            
            statsText += "• " + category + ": " + products.size() + " products, " +
                    categoryStock + " pcs., " + String.format("%.2f", categoryValue) + "₽\n";
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to admin panel button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Admin Panel");
        backButton.setCallbackData("admin_panel");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(statsText);
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show orders list for admin
     */
    public static void showOrdersList(MyTelegramBot bot, long chatId) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ You don't have administrator rights.");
            return;
        }
        
        List<Order> orders = OrderManager.getAllOrders();
        
        if (orders.isEmpty()) {
            bot.sendMessage(chatId, "📦 No orders yet.");
            return;
        }
        
        StringBuilder ordersText = new StringBuilder("📦 All Orders:\n\n");
        
        // Create product map for formatting
        Map<String, Product> productsMap = new HashMap<>();
        for (List<Product> productList : bot.getCategories().values()) {
            for (Product product : productList) {
                productsMap.put(product.getId(), product);
            }
        }
        
        for (Order order : orders) {
            ordersText.append(OrderManager.formatOrder(order, productsMap)).append("\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to admin panel button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Admin Panel");
        backButton.setCallbackData("admin_panel");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ordersText.toString());
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Refresh products (generate new random products)
     */
    public static void refreshProducts(MyTelegramBot bot, long chatId) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ You don't have administrator rights.");
            return;
        }
        
        // Clear existing products
        bot.getCategories().clear();
        
        // Generate new random products
        bot.getCategories().putAll(ProductGenerator.generateAllRandomProducts());
        
        // Add popular products
        bot.addPopularProducts();
        
        String refreshText = "🔄 *Products Updated!*\n\n" +
                "✨ Generated new random products for all categories\n" +
                "📊 Total Products: " + getTotalProductsCount(bot) + "\n\n" +
                "Categories updated:\n";
        
        for (String category : bot.getCategories().keySet()) {
            int count = bot.getCategories().get(category).size();
            refreshText += "• " + category + ": " + count + " products\n";
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to admin panel button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Admin Panel");
        backButton.setCallbackData("admin_panel");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(refreshText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private static int getTotalProductsCount(MyTelegramBot bot) {
        int total = 0;
        for (List<Product> products : bot.getCategories().values()) {
            total += products.size();
        }
        return total;
    }
}
