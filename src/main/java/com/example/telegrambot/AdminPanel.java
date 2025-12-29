package com.example.telegrambot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

/**
 * Класс для админ-панели управления товарами
 */
public class AdminPanel {
    
    // Список админов (в реальном приложении лучше хранить в базе данных)
    private static final Set<Long> ADMIN_IDS = new HashSet<>();
    
    static {
        // Добавьте сюда ID администраторов
        // ADMIN_IDS.add(123456789L);
    }
    
    /**
     * Проверить, является ли пользователь администратором
     */
    public static boolean isAdmin(long userId) {
        return ADMIN_IDS.contains(userId);
    }
    
    /**
     * Добавить администратора
     */
    public static void addAdmin(long userId) {
        ADMIN_IDS.add(userId);
    }
    
    /**
     * Удалить администратора
     */
    public static void removeAdmin(long userId) {
        ADMIN_IDS.remove(userId);
    }
    
    /**
     * Показать админ-панель
     */
    public static void showAdminPanel(MyTelegramBot bot, long chatId) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ У вас нет прав администратора.");
            return;
        }
        
        String adminText = "🔧 Админ-панель\n\n" +
                "Выберите действие:";
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопки управления товарами
        List<InlineKeyboardButton> productsRow = new ArrayList<>();
        InlineKeyboardButton addProductButton = new InlineKeyboardButton();
        addProductButton.setText("➕ Добавить товар");
        addProductButton.setCallbackData("admin_add_product");
        productsRow.add(addProductButton);
        
        InlineKeyboardButton listProductsButton = new InlineKeyboardButton();
        listProductsButton.setText("📋 Список товаров");
        listProductsButton.setCallbackData("admin_list_products");
        productsRow.add(listProductsButton);
        keyboard.add(productsRow);
        
        // Кнопки управления категориями
        List<InlineKeyboardButton> categoriesRow = new ArrayList<>();
        InlineKeyboardButton addCategoryButton = new InlineKeyboardButton();
        addCategoryButton.setText("📁 Добавить категорию");
        addCategoryButton.setCallbackData("admin_add_category");
        categoriesRow.add(addCategoryButton);
        
        InlineKeyboardButton listCategoriesButton = new InlineKeyboardButton();
        listCategoriesButton.setText("📂 Список категорий");
        listCategoriesButton.setCallbackData("admin_list_categories");
        categoriesRow.add(listCategoriesButton);
        keyboard.add(categoriesRow);
        
        // Кнопки статистики
        List<InlineKeyboardButton> statsRow = new ArrayList<>();
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("📊 Статистика");
        statsButton.setCallbackData("admin_stats");
        statsRow.add(statsButton);
        
        InlineKeyboardButton ordersButton = new InlineKeyboardButton();
        ordersButton.setText("📦 Заказы");
        ordersButton.setCallbackData("admin_orders");
        statsRow.add(ordersButton);
        keyboard.add(statsRow);
        
        // Кнопка обновления товаров
        List<InlineKeyboardButton> updateRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText("🔄 Обновить товары");
        updateButton.setCallbackData("admin_refresh_products");
        updateRow.add(updateButton);
        keyboard.add(updateRow);
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в меню");
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
     * Показать список всех товаров
     */
    public static void showProductsList(MyTelegramBot bot, long chatId, Map<String, List<Product>> categories) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ У вас нет прав администратора.");
            return;
        }
        
        StringBuilder productsText = new StringBuilder("📋 Список всех товаров:\n\n");
        
        for (Map.Entry<String, List<Product>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<Product> products = entry.getValue();
            
            productsText.append("📁 ").append(category).append(":\n");
            
            for (Product product : products) {
                productsText.append("• ").append(product.getName())
                        .append(" (ID: ").append(product.getId()).append(")")
                        .append(" - ").append(product.getPrice()).append("₽")
                        .append(" [Остаток: ").append(product.getStock()).append("]\n");
            }
            productsText.append("\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка "Назад в админ-панель"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-панель");
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
     * Показать список категорий
     */
    public static void showCategoriesList(MyTelegramBot bot, long chatId, Map<String, List<Product>> categories) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ У вас нет прав администратора.");
            return;
        }
        
        StringBuilder categoriesText = new StringBuilder("📂 Список категорий:\n\n");
        
        for (Map.Entry<String, List<Product>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<Product> products = entry.getValue();
            
            categoriesText.append("📁 ").append(category)
                    .append(" (").append(products.size()).append(" товаров)\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка "Назад в админ-панель"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-панель");
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
     * Показать статистику
     */
    public static void showStats(MyTelegramBot bot, long chatId, Map<String, List<Product>> categories) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ У вас нет прав администратора.");
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
        
        String statsText = "📊 Статистика магазина:\n\n" +
                "📦 Всего товаров: " + totalProducts + "\n" +
                "📋 Всего категорий: " + categories.size() + "\n" +
                "📦 Общий остаток: " + totalStock + " шт.\n" +
                "💰 Общая стоимость: " + String.format("%.2f", totalValue) + "₽\n\n" +
                "📁 По категориям:\n";
        
        for (Map.Entry<String, List<Product>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<Product> products = entry.getValue();
            int categoryStock = products.stream().mapToInt(Product::getStock).sum();
            double categoryValue = products.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
            
            statsText += "• " + category + ": " + products.size() + " товаров, " +
                    categoryStock + " шт., " + String.format("%.2f", categoryValue) + "₽\n";
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка "Назад в админ-панель"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-панель");
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
     * Показать список заказов для админа
     */
    public static void showOrdersList(MyTelegramBot bot, long chatId) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ У вас нет прав администратора.");
            return;
        }
        
        List<Order> orders = OrderManager.getAllOrders();
        
        if (orders.isEmpty()) {
            bot.sendMessage(chatId, "📦 Заказов пока нет.");
            return;
        }
        
        StringBuilder ordersText = new StringBuilder("📦 Все заказы:\n\n");
        
        // Создаем карту товаров для форматирования
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
        
        // Кнопка "Назад в админ-панель"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-панель");
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
     * Обновить товары (сгенерировать новые случайные товары)
     */
    public static void refreshProducts(MyTelegramBot bot, long chatId) {
        if (!isAdmin(chatId)) {
            bot.sendMessage(chatId, "❌ У вас нет прав администратора.");
            return;
        }
        
        // Очищаем существующие товары
        bot.getCategories().clear();
        
        // Генерируем новые случайные товары
        bot.getCategories().putAll(ProductGenerator.generateAllRandomProducts());
        
        // Добавляем популярные товары
        bot.addPopularProducts();
        
        String refreshText = "🔄 *Товары обновлены!*\n\n" +
                "✨ Сгенерированы новые случайные товары для всех категорий\n" +
                "📊 Общее количество товаров: " + getTotalProductsCount(bot) + "\n\n" +
                "Категории обновлены:\n";
        
        for (String category : bot.getCategories().keySet()) {
            int count = bot.getCategories().get(category).size();
            refreshText += "• " + category + ": " + count + " товаров\n";
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка "Назад в админ-панель"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад в админ-панель");
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
