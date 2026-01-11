package com.example.telegrambot;

import com.example.telegrambot.commands.CommandManager;
import com.example.telegrambot.factory.KeyboardFactory;
import com.example.telegrambot.payment.PaymentMethod;
import com.example.telegrambot.payment.PaymentMethodFactory;
import com.example.telegrambot.payment.PaymentProcessor;
import com.example.telegrambot.payment.PaymentResult;
import com.example.telegrambot.trading.AutoTradingEngine;
import com.example.telegrambot.trading.Trade;
import com.example.telegrambot.trading.TradingManager;
import com.example.telegrambot.trading.TradingStrategy;
import com.example.telegrambot.trading.TradingDecision;
import com.example.telegrambot.trading.WalletService;
import com.example.telegrambot.trading.NewCoinScanner;
import com.example.telegrambot.trading.PriceService;
import com.example.telegrambot.trading.DexAutoBuyService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

/**
 * Main Telegram bot class for online sales
 */
public class MyTelegramBot extends TelegramLongPollingBot {
    
    private final String BOT_USERNAME = Config.getBotUsername();
    private final String BOT_TOKEN = Config.getBotToken();
    
    // Payment group link
    private static final String PAYMENT_GROUP_LINK = "https://t.me/+MMkALipObugzNjNi";
    
    // Command manager (Command pattern)
    private final CommandManager commandManager;
    
    // User carts storage
    private final Map<Long, Cart> userCarts = new HashMap<>();
    
    // Product catalog
    private final Map<String, List<Product>> categories = new HashMap<>();
    
    // Pending orders for payment method selection
    private final Map<Long, String> pendingOrders = new HashMap<>();
    
    public MyTelegramBot() {
        this.commandManager = new CommandManager(this);
        initializeProducts();
        
        // Автоматически запускаем торговлю при старте бота
        try {
            AutoTradingEngine engine = AutoTradingEngine.getInstance();
            engine.setBot(this); // Устанавливаем бота для уведомлений
            if (!engine.isRunning()) {
                engine.start();
                System.out.println("✅ Автоматическая торговля запущена при старте бота");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Ошибка при запуске автоматической торговли: " + e.getMessage());
        }
    }
    
    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }
    
    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            
            // Проверяем, не является ли это адресом для вывода
            if (pendingWithdrawAmounts.containsKey(chatId)) {
                handleWithdrawAddress(chatId, messageText);
                return;
            }
            
            // Проверяем, не является ли это суммой для пополнения
            try {
                double amount = Double.parseDouble(messageText);
                if (amount > 0 && amount <= 100000) {
                    pendingDepositAmounts.put(chatId, amount);
                    handleDepositAmount(chatId, String.valueOf(amount));
                    return;
                }
            } catch (NumberFormatException e) {
                // Не число, продолжаем обычную обработку
            }
            
            // Try to process through CommandManager first (Command pattern)
            if (!commandManager.executeCommand(messageText, chatId)) {
                // If not a known command, try to process as search query
                if (messageText.length() > 1 && !messageText.startsWith("/")) {
                    searchProducts(chatId, messageText);
                } else {
                    sendMessage(chatId, "Use menu buttons or type product name to search");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            
            handleCallbackQuery(chatId, callbackData);
        }
    }
    
    private void sendWelcomeMessage(long chatId) {
        // ПРИНУДИТЕЛЬНО удаляем старую клавиатуру
        try {
            SendMessage removeMsg1 = new SendMessage();
            removeMsg1.setChatId(chatId);
            ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
            removeKeyboard.setRemoveKeyboard(true);
            removeMsg1.setReplyMarkup(removeKeyboard);
            execute(removeMsg1);
            Thread.sleep(300);
            
            // Отправляем пустое сообщение для очистки
            SendMessage removeMsg2 = new SendMessage();
            removeMsg2.setChatId(chatId);
            removeMsg2.setText(" ");
            removeMsg2.setReplyMarkup(removeKeyboard);
            execute(removeMsg2);
            Thread.sleep(300);
        } catch (Exception e) {
            // Игнорируем
        }
        
        String welcomeText = "🎉 *Добро пожаловать!*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "💰 *Автоматическая торговля криптовалютой*\n\n" +
                "✨ *Возможности:*\n" +
                "🤖 Автоматическая торговля\n" +
                "🚀 Обнаружение пампов\n" +
                "🆕 Торговля новыми монетами\n" +
                "💰 Управление кошельком\n" +
                "🛒 Авто-закупка новых токенов\n" +
                "📊 Детальная статистика\n\n" +
                "👤 *Начните с личного кабинета!*\n\n" +
                "💡 *Быстрый старт:*\n" +
                "1️⃣ Пополните баланс\n" +
                "2️⃣ Запустите торговлю\n" +
                "3️⃣ Получайте прибыль автоматически";
        
        // Создаем НОВУЮ клавиатуру с правильными кнопками
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
            
            // Отправляем еще одно сообщение с клавиатурой для принудительного обновления
            Thread.sleep(500);
            SendMessage forceUpdate = new SendMessage();
            forceUpdate.setChatId(chatId);
            forceUpdate.setText("✅ *Меню обновлено!*\n\nНажмите кнопку: 🤖 Авто-торговля");
            forceUpdate.setParseMode("Markdown");
            forceUpdate.setReplyMarkup(keyboard);
            execute(forceUpdate);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void sendMainMenu(long chatId) {
        // Get all products from all categories
        List<Product> allProducts = new ArrayList<>();
        for (List<Product> productList : categories.values()) {
            allProducts.addAll(productList);
        }
        
        if (allProducts.isEmpty()) {
            sendMessage(chatId, "No products available in the store yet.");
            return;
        }
        
        // Create beautiful showcase of all products
        StringBuilder showcaseText = new StringBuilder();
        showcaseText.append("🛍️ *Fredo Store - All Products*\n\n");
        showcaseText.append("Choose a product to add to cart:\n\n");
        
        // Group products by 2 per row for beautiful display
        for (int i = 0; i < allProducts.size(); i += 2) {
            Product product1 = allProducts.get(i);
            showcaseText.append(formatProductForShowcase(product1));
            
            if (i + 1 < allProducts.size()) {
                Product product2 = allProducts.get(i + 1);
                showcaseText.append(" | ").append(formatProductForShowcase(product2));
            }
            showcaseText.append("\n\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Create buttons for each product
        for (int i = 0; i < allProducts.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            // First product in row
            Product product1 = allProducts.get(i);
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("🛒 " + product1.getName());
            button1.setCallbackData("product_" + product1.getId());
            row.add(button1);
            
            // Second product in row (if exists)
            if (i + 1 < allProducts.size()) {
                Product product2 = allProducts.get(i + 1);
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("🛒 " + product2.getName());
                button2.setCallbackData("product_" + product2.getId());
                row.add(button2);
            }
            
            keyboard.add(row);
        }
        
        // Add cart button
        List<InlineKeyboardButton> bottomRow = new ArrayList<>();
        InlineKeyboardButton cartButton = new InlineKeyboardButton();
        cartButton.setText("🛒 Cart");
        cartButton.setCallbackData("cart");
        bottomRow.add(cartButton);
        keyboard.add(bottomRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(showcaseText.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    
    private String formatProductForShowcase(Product product) {
        return String.format("**%s** %s\n%s\n💰 %d₽", 
            product.getName(), 
            getProductEmoji(product.getName()),
            product.getDescription(),
            (int)product.getPrice()
        );
    }
    
    private String getProductEmoji(String productName) {
        // Return corresponding emoji for product
        if (productName.contains("Пицца")) return "🍕";
        if (productName.contains("Чизбургер")) return "🍔";
        if (productName.contains("Капучино")) return "☕";
        if (productName.contains("Ролл")) return "🍣";
        if (productName.contains("iPhone") || productName.contains("Samsung")) return "📱";
        if (productName.contains("MacBook")) return "💻";
        if (productName.contains("AirPods")) return "🎧";
        if (productName.contains("Watch")) return "⌚";
        if (productName.contains("Футболка")) return "👕";
        if (productName.contains("Джинсы")) return "👖";
        if (productName.contains("Куртка")) return "🧥";
        if (productName.contains("кроссовки") || productName.contains("Nike")) return "👟";
        if (productName.contains("Платье")) return "👗";
        if (productName.contains("лампа")) return "💡";
        if (productName.contains("Фикус") || productName.contains("растение")) return "🌿";
        if (productName.contains("свеча")) return "🕯️";
        if (productName.contains("подушка")) return "🛋️";
        if (productName.contains("PlayStation")) return "🎮";
        if (productName.contains("Гарри Поттер") || productName.contains("книг")) return "📚";
        if (productName.contains("Netflix")) return "🎬";
        if (productName.contains("пазл")) return "🧩";
        if (productName.contains("Ecstasy") || productName.contains("MDMA")) return "💊💃🕺";
        if (productName.contains("LSD") || productName.contains("кислота")) return "🍭";
        if (productName.contains("Weed") || productName.contains("марихуана") || productName.contains("травка")) return "🌲🍌";
        if (productName.contains("Cocaine") || productName.contains("кокаин")) return "🥥";
        if (productName.contains("духи") || productName.contains("Chanel")) return "💄";
        if (productName.contains("крем")) return "🧴";
        if (productName.contains("витамины")) return "💊";
        if (productName.contains("маска")) return "🎭";
        
        return "📦"; // Default emoji
    }
    
    private void sendProductDetails(long chatId, String productId) {
        Product product = findProductById(productId);
        if (product == null) {
            sendMessage(chatId, "Product not found.");
            return;
        }
        
        String productText = "🛍️ *" + product.getName() + "*\n\n" +
                getProductEmoji(product.getName()) + " *Description:* " + product.getDescription() + "\n\n" +
                "💰 *Price:* " + (int)product.getPrice() + "₽\n" +
                "📦 *In Stock:* " + product.getStock() + " pcs.\n" +
                "🏷️ *Category:* " + product.getCategory() + "\n\n" +
                "✨ *Want to add to cart?*";
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Buttons for adding to cart
        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("🛒 Add to Cart");
        addButton.setCallbackData("add_to_cart_" + productId);
        addRow.add(addButton);
        keyboard.add(addRow);
        
        // Back button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Products");
        backButton.setCallbackData("back_to_main_menu");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(productText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendCart(long chatId) {
        Cart cart = userCarts.getOrDefault(chatId, new Cart());
        
        if (cart.getItems().isEmpty()) {
            String emptyCartText = "🛒 *Your cart is empty*\n\n" +
                    "✨ Add products from catalog to place an order!\n\n" +
                    "Click the button below to browse products:";
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> shopRow = new ArrayList<>();
            InlineKeyboardButton shopButton = new InlineKeyboardButton();
            shopButton.setText("🛍️ Browse Products");
            shopButton.setCallbackData("back_to_main_menu");
            shopRow.add(shopButton);
            keyboard.add(shopRow);
            
            markup.setKeyboard(keyboard);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(emptyCartText);
            message.setParseMode("Markdown");
            message.setReplyMarkup(markup);
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        
        StringBuilder cartText = new StringBuilder("🛒 *Your Cart*\n\n");
        double total = 0;
        int itemCount = 0;
        
        for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
            Product product = findProductById(entry.getKey());
            if (product != null) {
                double itemTotal = product.getPrice() * entry.getValue();
                total += itemTotal;
                itemCount += entry.getValue();
                
                cartText.append(getProductEmoji(product.getName()))
                        .append(" *").append(product.getName()).append("*\n")
                        .append("   Quantity: ").append(entry.getValue()).append(" pcs.\n")
                        .append("   Price: ").append(String.format("%.0f", itemTotal)).append("₽\n\n");
            }
        }
        
        cartText.append("📊 *Total Items:* ").append(itemCount).append(" pcs.\n")
                .append("💰 *Total Amount:* ").append(String.format("%.0f", total)).append("₽\n\n")
                .append("✨ *Ready to checkout?*");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Checkout button
        List<InlineKeyboardButton> orderRow = new ArrayList<>();
        InlineKeyboardButton orderButton = new InlineKeyboardButton();
        orderButton.setText("✅ Checkout");
        orderButton.setCallbackData("checkout");
        orderRow.add(orderButton);
        keyboard.add(orderRow);
        
        // Cart management buttons
        List<InlineKeyboardButton> manageRow = new ArrayList<>();
        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("🗑️ Clear Cart");
        clearButton.setCallbackData("clear_cart");
        manageRow.add(clearButton);
        
        InlineKeyboardButton continueButton = new InlineKeyboardButton();
        continueButton.setText("🛍️ Continue Shopping");
        continueButton.setCallbackData("back_to_main_menu");
        manageRow.add(continueButton);
        keyboard.add(manageRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(cartText.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendOrders(long chatId) {
        List<Order> orders = OrderManager.getUserOrders(chatId);
        
        if (orders.isEmpty()) {
            sendMessage(chatId, "📋 Your Orders:\n\nNo orders yet. Place your first order through the cart!");
            return;
        }
        
        StringBuilder ordersText = new StringBuilder("📋 Your Orders:\n\n");
        
        // Create product map for formatting
        Map<String, Product> productsMap = new HashMap<>();
        for (List<Product> productList : categories.values()) {
            for (Product product : productList) {
                productsMap.put(product.getId(), product);
            }
        }
        
        for (Order order : orders) {
            ordersText.append(OrderManager.formatOrder(order, productsMap)).append("\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to menu button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Menu");
        backButton.setCallbackData("back_to_main_menu");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(ordersText.toString());
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendHelp(long chatId) {
        String helpText = "❓ *Bot Help Guide:*\n\n" +
                "🛍️ *Main Commands:*\n" +
                "/start - Start using the bot\n" +
                "/menu - Open product catalog\n" +
                "/cart - View shopping cart\n" +
                "/orders - View your orders\n" +
                "/help - Show this help\n\n" +
                "🔍 *Product Search:*\n" +
                "Use the 'Search Products' button for quick search by name or description\n\n" +
                "🛒 *How to Order:*\n" +
                "1. Browse the product catalog\n" +
                "2. Add products to cart\n" +
                "3. Checkout\n\n" +
                "📞 *Support:* 24/7";
        
        if (AdminPanel.isAdmin(chatId)) {
            helpText += "\n\n🔧 *Admin Commands:*\n/admin - Admin Panel";
        }
        
        helpText += "\n\nUse menu buttons for navigation.";
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Menu");
        backButton.setCallbackData("back_to_main_menu");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(helpText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendSearchPrompt(long chatId) {
        String text = "Select a category to search:";
        
        InlineKeyboardMarkup markup = KeyboardFactory.createSearchCategoriesKeyboard();
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void searchProducts(long chatId, String query) {
        List<Product> searchResults = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        // Search through all products
        for (List<Product> productList : categories.values()) {
            for (Product product : productList) {
                if (product.getName().toLowerCase().contains(lowerQuery) ||
                    product.getDescription().toLowerCase().contains(lowerQuery) ||
                    product.getCategory().toLowerCase().contains(lowerQuery)) {
                    searchResults.add(product);
                }
            }
        }
        
        if (searchResults.isEmpty()) {
            String noResultsText = "🔍 *Search Results*\n\n" +
                    "❌ No results found for \"" + query + "\".\n\n" +
                    "💡 *Try:*\n" +
                    "• Change your search query\n" +
                    "• Use more general words\n" +
                    "• Check spelling";
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Back to Menu");
            backButton.setCallbackData("back_to_main_menu");
            backRow.add(backButton);
            keyboard.add(backRow);
            
            markup.setKeyboard(keyboard);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(noResultsText);
            message.setParseMode("Markdown");
            message.setReplyMarkup(markup);
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        
        // Show search results
        StringBuilder searchText = new StringBuilder();
        searchText.append("🔍 *Search Results for: \"").append(query).append("\"*\n\n");
        searchText.append("Found products: ").append(searchResults.size()).append("\n\n");
        
        // Group results by 2 per row
        for (int i = 0; i < searchResults.size(); i += 2) {
            Product product1 = searchResults.get(i);
            searchText.append(formatProductForShowcase(product1));
            
            if (i + 1 < searchResults.size()) {
                Product product2 = searchResults.get(i + 1);
                searchText.append(" | ").append(formatProductForShowcase(product2));
            }
            searchText.append("\n\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Create buttons for each found product
        for (int i = 0; i < searchResults.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            Product product1 = searchResults.get(i);
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("🛒 " + product1.getName());
            button1.setCallbackData("product_" + product1.getId());
            row.add(button1);
            
            if (i + 1 < searchResults.size()) {
                Product product2 = searchResults.get(i + 1);
                InlineKeyboardButton button2 = new InlineKeyboardButton();
                button2.setText("🛒 " + product2.getName());
                button2.setCallbackData("product_" + product2.getId());
                row.add(button2);
            }
            
            keyboard.add(row);
        }
        
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
        message.setText(searchText.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleCallbackQuery(long chatId, String callbackData) {
        if (callbackData.startsWith("product_")) {
            String productId = callbackData.substring(8);
            sendProductDetails(chatId, productId);
        } else if (callbackData.startsWith("add_to_cart_")) {
            String productId = callbackData.substring(12);
            addToCart(chatId, productId);
        } else if (callbackData.equals("cart")) {
            sendCart(chatId);
        } else if (callbackData.equals("back_to_categories") || callbackData.equals("back_to_main_menu")) {
            sendMainMenu(chatId);
        } else if (callbackData.startsWith("back_to_category_")) {
            sendMainMenu(chatId);
        } else if (callbackData.equals("checkout")) {
            processCheckout(chatId);
        } else if (callbackData.startsWith("payment_crypto_")) {
            String orderId = callbackData.substring(15);
            processPayment(chatId, orderId, PaymentMethodFactory.PaymentType.CRYPTO);
        } else if (callbackData.startsWith("payment_stars_")) {
            String orderId = callbackData.substring(14);
            processPayment(chatId, orderId, PaymentMethodFactory.PaymentType.STARS);
        } else if (callbackData.equals("clear_cart")) {
            clearCart(chatId);
        } else if (callbackData.equals("admin_panel")) {
            AdminPanel.showAdminPanel(this, chatId);
        } else if (callbackData.equals("admin_list_products")) {
            AdminPanel.showProductsList(this, chatId, categories);
        } else if (callbackData.equals("admin_list_categories")) {
            AdminPanel.showCategoriesList(this, chatId, categories);
        } else if (callbackData.equals("admin_stats")) {
            AdminPanel.showStats(this, chatId, categories);
        } else if (callbackData.equals("admin_orders")) {
            AdminPanel.showOrdersList(this, chatId);
        } else if (callbackData.equals("admin_refresh_products")) {
            AdminPanel.refreshProducts(this, chatId);
        } else if (callbackData.startsWith("search_category_")) {
            String category = callbackData.substring(16);
            handleSearchCategory(chatId, category);
        } else if (callbackData.equals("back_to_search")) {
            sendSearchPrompt(chatId);
        } else if (callbackData.equals("trading_start")) {
            handleTradingStart(chatId);
        } else if (callbackData.equals("trading_stop")) {
            handleTradingStop(chatId);
        } else if (callbackData.equals("trading_stats")) {
            handleTradingStats(chatId);
        } else if (callbackData.equals("trading_trades")) {
            handleTradingTrades(chatId);
        } else if (callbackData.equals("trading_strategies")) {
            handleTradingStrategies(chatId);
        } else if (callbackData.equals("trading_notifications")) {
            handleTradingNotifications(chatId);
        } else if (callbackData.equals("trading_back")) {
            commandManager.executeCommand("🤖 Авто-торговля", chatId);
        } else if (callbackData.equals("refresh_coin_listing")) {
            commandManager.executeCommand("📊 Листинг монет", chatId);
        } else if (callbackData.equals("wallet_deposit")) {
            handleWalletDeposit(chatId);
        } else if (callbackData.equals("wallet_withdraw")) {
            handleWalletWithdraw(chatId);
        } else if (callbackData.equals("wallet_history")) {
            handleWalletHistory(chatId);
        } else if (callbackData.equals("wallet_refresh")) {
            commandManager.executeCommand("💰 Кошелек", chatId);
        } else if (callbackData.startsWith("deposit_amount_")) {
            String amountStr = callbackData.substring(15);
            handleDepositAmount(chatId, amountStr);
        } else if (callbackData.startsWith("deposit_method_")) {
            String method = callbackData.substring(15);
            handleDepositMethod(chatId, method);
        } else if (callbackData.startsWith("withdraw_amount_")) {
            String amountStr = callbackData.substring(17);
            handleWithdrawAmount(chatId, amountStr);
        } else if (callbackData.equals("wallet_back")) {
            commandManager.executeCommand("💰 Кошелек", chatId);
        } else if (callbackData.equals("deposit_custom")) {
            sendMessage(chatId, "💵 *Другая сумма*\n\n" +
                "Пожалуйста, отправьте сумму для пополнения в следующем сообщении.\n" +
                "Формат: просто число, например: 250");
        } else if (callbackData.equals("cabinet_trading")) {
            commandManager.executeCommand("🤖 Авто-торговля", chatId);
        } else if (callbackData.equals("cabinet_new_coins")) {
            handleNewCoins(chatId);
        } else if (callbackData.equals("cabinet_settings")) {
            handleSettings(chatId);
        } else if (callbackData.equals("cabinet_refresh")) {
            commandManager.executeCommand("👤 Личный кабинет", chatId);
        } else if (callbackData.startsWith("scan_new_coins_")) {
            handleScanNewCoins(chatId);
        } else if (callbackData.startsWith("analyze_coin_")) {
            String symbol = callbackData.substring(13);
            handleAnalyzeCoin(chatId, symbol);
        } else if (callbackData.equals("scan_new_coins_now")) {
            handleScanNewCoins(chatId);
        } else if (callbackData.equals("enable_auto_scan")) {
            sendMessage(chatId, "✅ Авто-сканирование новых монет включено!\n\n" +
                "Система будет автоматически проверять новые листинги и входить в перспективные монеты.");
        } else if (callbackData.equals("cabinet_back")) {
            commandManager.executeCommand("👤 Личный кабинет", chatId);
        } else if (callbackData.startsWith("trade_coin_")) {
            String symbol = callbackData.substring(11);
            handleTradeCoin(chatId, symbol);
        } else if (callbackData.equals("autobuy_start")) {
            handleAutoBuyStart(chatId);
        } else if (callbackData.equals("autobuy_stop")) {
            handleAutoBuyStop(chatId);
        } else if (callbackData.equals("autobuy_stats")) {
            handleAutoBuyStats(chatId);
        }
    }
    
    private void handleWithdrawAddress(long chatId, String address) {
        Double amount = pendingWithdrawAmounts.remove(chatId);
        if (amount == null) {
            sendMessage(chatId, "❌ Сумма не выбрана. Начните заново.");
            return;
        }
        
        if (address == null || address.trim().isEmpty() || address.length() < 10) {
            sendMessage(chatId, "❌ Неверный адрес. Пожалуйста, отправьте корректный адрес кошелька.");
            pendingWithdrawAmounts.put(chatId, amount);
            return;
        }
        
        WalletService.WithdrawResult result = WalletService.withdraw(chatId, amount, "CRYPTO", address.trim());
        
        if (result.success) {
            sendMessage(chatId, "✅ *Запрос на вывод создан*\n\n" +
                "💸 Сумма: " + String.format("%.2f", amount) + " USDT\n" +
                "📍 Адрес: " + address + "\n" +
                "🆔 ID транзакции: " + (result.transactionId != null ? result.transactionId : "N/A") + "\n\n" +
                "💵 Новый баланс: " + String.format("%.2f", result.newBalance) + " USDT\n\n" +
                "⏳ Обработка займет некоторое время.");
        } else {
            sendMessage(chatId, "❌ Ошибка вывода: " + result.message);
        }
    }
    
    private void addToCart(long chatId, String productId) {
        Product product = findProductById(productId);
        if (product == null) {
            sendMessage(chatId, "Product not found.");
            return;
        }
        
        Cart cart = userCarts.getOrDefault(chatId, new Cart());
        cart.addItem(productId, 1);
        userCarts.put(chatId, cart);
        
        sendMessage(chatId, "✅ " + product.getName() + " added to cart!");
    }
    
    private void processCheckout(long chatId) {
        Cart cart = userCarts.getOrDefault(chatId, new Cart());
        if (cart.getItems().isEmpty()) {
            sendMessage(chatId, "Cart is empty!");
            return;
        }
        
        // Calculate total amount
        double totalAmount = 0;
        for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
            Product product = findProductById(entry.getKey());
            if (product != null) {
                totalAmount += product.getPrice() * entry.getValue();
            }
        }
        
        // Create order
        String orderId = OrderManager.createOrder(chatId, cart.getItems(), totalAmount);
        pendingOrders.put(chatId, orderId);
        
        // Show payment method selection
        showPaymentMethodSelection(chatId, orderId, totalAmount);
    }
    
    private void showPaymentMethodSelection(long chatId, String orderId, double totalAmount) {
        String messageText = "✅ Order #" + orderId + " created!\n" +
                "💰 Amount: " + String.format("%.2f", totalAmount) + "₽\n\n" +
                "💳 Select payment method:";
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Cryptocurrency payment button
        List<InlineKeyboardButton> cryptoRow = new ArrayList<>();
        InlineKeyboardButton cryptoButton = new InlineKeyboardButton();
        cryptoButton.setText("₿ Cryptocurrency");
        cryptoButton.setCallbackData("payment_crypto_" + orderId);
        cryptoRow.add(cryptoButton);
        keyboard.add(cryptoRow);
        
        // Telegram Stars payment button
        List<InlineKeyboardButton> starsRow = new ArrayList<>();
        InlineKeyboardButton starsButton = new InlineKeyboardButton();
        starsButton.setText("⭐ Telegram Stars");
        starsButton.setCallbackData("payment_stars_" + orderId);
        starsRow.add(starsButton);
        keyboard.add(starsRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void processPayment(long chatId, String orderId, PaymentMethodFactory.PaymentType paymentType) {
        Order order = OrderManager.getOrder(orderId);
        if (order == null) {
            sendMessage(chatId, "❌ Order not found!");
            return;
        }
        
        double totalAmount = order.getTotalAmount();
        
        // Create payment processor with selected method
        PaymentMethod paymentMethod = PaymentMethodFactory.create(paymentType);
        PaymentProcessor processor = new PaymentProcessor(paymentMethod);
        
        // Process payment
        PaymentResult result = processor.process(orderId, totalAmount);
        
        if (result.isSuccess()) {
            // Successful payment - show payment link
            String successMessage = "✅ Order #" + orderId + " created!\n" +
                    "💰 Amount: " + String.format("%.2f", totalAmount) + "₽\n" +
                    "💳 Payment Method: " + paymentMethod.getEmoji() + " " + paymentMethod.getMethodName() + "\n\n" +
                    "🔗 Follow the link to complete payment:";
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> paymentRow = new ArrayList<>();
            InlineKeyboardButton paymentButton = new InlineKeyboardButton();
            paymentButton.setText("🔗 Go to Payment");
            paymentButton.setUrl(PAYMENT_GROUP_LINK);
            paymentRow.add(paymentButton);
            keyboard.add(paymentRow);
            
            markup.setKeyboard(keyboard);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(successMessage);
            message.setReplyMarkup(markup);
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            
            // Clear cart after successful order
            Cart cart = userCarts.getOrDefault(chatId, new Cart());
            cart.clear();
            userCarts.put(chatId, cart);
            pendingOrders.remove(chatId);
        } else {
            // Payment error - refund notification, no link shown
            String errorMessage = "❌ Payment processing error via " + paymentMethod.getMethodName() + "!\n\n" +
                    "💰 Order #" + orderId + " cancelled.\n" +
                    "💵 Funds will be automatically refunded within 24 hours.\n\n" +
                    "Please try again later or contact support.";
            
            sendMessage(chatId, errorMessage);
            
            // Cancel order
            order.setStatus(Order.OrderStatus.CANCELLED);
            pendingOrders.remove(chatId);
        }
    }
    
    
    private void clearCart(long chatId) {
        Cart cart = userCarts.getOrDefault(chatId, new Cart());
        cart.clear();
        userCarts.put(chatId, cart);
        sendMessage(chatId, "🗑️ Cart cleared.");
    }
    
    private Product findProductById(String productId) {
        for (List<Product> productList : categories.values()) {
            for (Product product : productList) {
                if (product.getId().equals(productId)) {
                    return product;
                }
            }
        }
        return null;
    }
    
    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        
        // ВСЕГДА возвращаем клавиатуру после каждого сообщения
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, List<Product>> getCategories() {
        return categories;
    }
    
    private void initializeProducts() {
        // Initialize categories
        categories.put("🍕 Еда и напитки", new ArrayList<>());
        categories.put("📱 Электроника", new ArrayList<>());
        categories.put("👕 Одежда и обувь", new ArrayList<>());
        categories.put("🏠 Дом и сад", new ArrayList<>());
        categories.put("🎮 Развлечения", new ArrayList<>());
        categories.put("💄 Красота и здоровье", new ArrayList<>());
        
        // Add only our main products
        addPopularProducts();
    }
    
    public void addPopularProducts() {
        // Add only main products
        
        // 🍕 Еда и напитки
        List<Product> food = categories.get("🍕 Еда и напитки");
        food.add(0, new Product("pizza_margherita", "Пицца Маргарита", "🍕 Классическая итальянская пицца", 450, 25, "🍕 Еда и напитки"));
        food.add(1, new Product("coffee_cappuccino", "Капучино", "☕ Ароматный кофе", 120, 50, "🍕 Еда и напитки"));
        
        // 📱 Электроника
        List<Product> electronics = categories.get("📱 Электроника");
        electronics.add(0, new Product("iphone_15_pro", "iPhone 15 Pro", "📱 Новейший смартфон", 99999, 8, "📱 Электроника"));
        electronics.add(1, new Product("macbook_pro_m3", "MacBook Pro M3", "💻 Мощный ноутбук", 149999, 5, "📱 Электроника"));
        
        // 👕 Одежда и обувь
        List<Product> clothing = categories.get("👕 Одежда и обувь");
        clothing.add(0, new Product("nike_air_max", "Nike Air Max", "👟 Кроссовки", 12999, 25, "👕 Одежда и обувь"));
        clothing.add(1, new Product("levis_501", "Джинсы Levis 501", "👖 Классические джинсы", 5999, 30, "👕 Одежда и обувь"));
        
        // 🏠 Дом и сад
        List<Product> home = categories.get("🏠 Дом и сад");
        home.add(0, new Product("led_lamp", "LED Лампа", "💡 Современная лампа", 2500, 15, "🏠 Дом и сад"));
        home.add(1, new Product("aroma_candle", "Ароматическая свеча", "🕯️ Свеча с ароматом лаванды", 800, 40, "🏠 Дом и сад"));
        
        // 🎮 Развлечения
        List<Product> entertainment = categories.get("🎮 Развлечения");
        entertainment.add(0, new Product("playstation_5", "PlayStation 5", "🎮 Игровая консоль", 49999, 3, "🎮 Развлечения"));
        entertainment.add(1, new Product("netflix_premium", "Netflix Premium", "🎬 Подписка на 3 месяца", 1500, 100, "🎮 Развлечения"));
        
        // 💄 Красота и здоровье
        List<Product> beauty = categories.get("💄 Красота и здоровье");
        beauty.add(0, new Product("chanel_no5", "Chanel №5", "💄 Легендарные духи", 15000, 8, "💄 Красота и здоровье"));
        beauty.add(1, new Product("face_cream", "Крем для лица", "🧴 Увлажняющий крем", 3200, 25, "💄 Красота и здоровье"));
    }
    
    private void sendHeader(long chatId) {
        String text = "👽\n\nhttps://f1.tf/Inoplaneteane сайт 👽\n\n" +
                "@BLSH7 @BLSH7Bot 🍫☘❄️🥥🪬🍬\n" +
                "====================\n" +
                "@Fredo_MarketMD \n" +
                "@MarketMD_FSBOT\n" +
                "@FSMD_RC 🍫☘🥥\n" +
                "====================\n" +
                "@A4R4M @A4R4Mbot 🍫☘🥥\n" +
                "====================\n" +
                "@BOBFOREVERTRUST\n" +
                "@BoboTrustForever_bot 🍫☘🥥\n" +
                "====================\n" +
                "@KandidatMD 🌲🍫🥥\n" +
                "====================\n" +
                "@PortMNC_RMD\n" +
                "@MonacoMD_BOT 🍫☘🥥\n" +
                "====================\n" +
                "@MARASLTMD @MS13MDbot 🍫☘❄️🥥🪬🍬\n" +
                "====================\n" +
                "@ZVDMD @MDZVDbot 🍫☘🥥\n" +
                "====================\n" +
                "@freshdr_777 @Fresh_dr777_bot 🍫🥥\n" +
                "====================\n" +
                "@N4N6N8 @MDNASAbot 🍫☘🥥\n" +
                "====================\n" +
                "@KrystaL337MD\n" +
                "@KrystaLMD373bot \n" +
                "🍫☘❄️🥥\n" +
                "====================\n" +
                "@MrGreenNew☘🍫\n" +
                "@MRGRNBOT 👽\n" +
                "====================\n" +
                "@MarShmell09 @Mell09Bot 🍫❄️☘\n" +
                "====================\n" +
                "@Gr22nQueeN @queenstrbot 🍫❄️☘\n" +
                "====================\n" +
                "@ZoroTopZzZ\n" +
                "@ZoroTopZzZoperZzZ\n" +
                "@ZorroTopBot  ❄️🍫🍀\n" +
                "====================\n" +
                "@YO25SHOP 🍭\n" +
                "====================\n" +
                "@BELLUCCIMD 🍫🍀🥥\n" +
                "====================\n" +
                "@WWONCA @wwonca_bot🍫🍀🥥 \n" +
                "====================\n" +
                "@primeultra_bot 🤖 \n" +
                "@SUPPRIME01 🍀\n" +
                "@SuperPrimeUltra 🍫🥥\n" +
                "====================\n" +
                "@DeiLmd @DeiLmd_bot 🍫🍀\n" +
                "====================\n" +
                "@mzpapa @moncler999bot\n" +
                "@mzreklama ❗️🥥 🌲🍫\n" +
                "====================\n" +
                "@smoky2bot 🍬\n" +
                "@smokymo_operator 🌲🍫\n" +
                "====================\n" +
                "@MARY_WEED 🥥🍫❄️💊\n" +
                "====================\n\n" +
                "👽💰\n" +
                "@BLackCatEx \n" +
                "@TheMatrixEx \n" +
                "@CryptuLMDrsrv \n" +
                "@CandyEXC   \n" +
                "@FRN_Crypto1 \n" +
                "@Monkeys_Crypto1  \n" +
                "@BTCBOSSMD  \n" +
                "@BLACKROCKEX \n" +
                "@HCHANGE1 \n" +
                "@Trust_LTC \n" +
                "@LTC_MAKLER \n" +
                "@StichLtc \n" +
                "@CryptoCOBA \n" +
                "@HiroshimaExc  \n" +
                "@PROFESOR_EX\n" +
                "@GoldXCHG\n" +
                "@mvp_exchange\n" +
                "@KryptoMahNEW\n" +
                "@GhostCryptoMD\n" +
                "@Lustig_LTC777\n" +
                "@ACHiLLES_LTC\n" +
                "@MIKE_LTC2\n" +
                "@LesbeaEX\n\n" +
                "@d3s1gngun 👨‍🎤👽 - дизайн 🗣\n\n" +
                "https://f1.tf/Inoplaneteane сайт 👽\n\n" +
                "https://signal.group/#CjQKIAVBCoKJ9vWuON7wq8EB1eHTIx7zHwyY7pZgQ9ALVSWwEhD8XPcX0W4crk30nOe-1glD";
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        // Remove Markdown parsing to avoid errors
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleSearchCategory(long chatId, String category) {
        String text = "";
        String categoryName = "";
        
        switch (category) {
            case "гаш":
                categoryName = "Hash/Bud 🍫🥦";
                text = "🔍 *Search by Category: " + categoryName + "*\n\n" +
                       "Found products in Hash/Bud category:\n\n" +
                       "🍫 **Premium Hash** - 2,500₽\n" +
                       "🥦 **A+ Buds** - 1,800₽\n" +
                       "🍫 **Dutch Hash** - 3,000₽\n" +
                       "🥦 **Indica Buds** - 2,200₽\n\n" +
                       "Select a product to add to cart!";
                break;
            case "cox":
                categoryName = "Cox 🥥";
                text = "🔍 *Search by Category: " + categoryName + "*\n\n" +
                       "Found products in Cox category:\n\n" +
                       "🥥 **White Cox** - 1,200₽\n" +
                       "🥥 **Peruvian Cox** - 1,500₽\n" +
                       "🥥 **Colombian Cox** - 1,800₽\n\n" +
                       "Select a product to add to cart!";
                break;
            case "lsd":
                categoryName = "LSD 🍭🍄";
                text = "🔍 *Search by Category: " + categoryName + "*\n\n" +
                       "Found products in LSD category:\n\n" +
                       "🍭 **LSD-25** - 800₽\n" +
                       "🍄 **Psilocybin Mushrooms** - 1,000₽\n" +
                       "🍭 **LSD-100** - 1,200₽\n" +
                       "🍄 **Golden Mushrooms** - 1,500₽\n\n" +
                       "Select a product to add to cart!";
                break;
            case "ice":
                categoryName = "❄️⚡";
                text = "🔍 *Search by Category: " + categoryName + "*\n\n" +
                       "Found products in Ice category:\n\n" +
                       "❄️ **Crystal Ice** - 2,000₽\n" +
                       "⚡ **Speed** - 1,500₽\n" +
                       "❄️ **Blue Ice** - 2,500₽\n\n" +
                       "Select a product to add to cart!";
                break;
            case "pills":
                categoryName = "💊💎";
                text = "🔍 *Search by Category: " + categoryName + "*\n\n" +
                       "Found products in Pills category:\n\n" +
                       "💊 **Ecstasy** - 800₽\n" +
                       "💎 **MDMA** - 1,200₽\n" +
                       "💊 **Amphetamine** - 1,000₽\n" +
                       "💎 **Crystal** - 1,800₽\n\n" +
                       "Select a product to add to cart!";
                break;
            case "empty":
                text = "❌ This button is not active yet";
                break;
            default:
                text = "❌ Category not found";
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Back to search button
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Back to Search");
        backButton.setCallbackData("back_to_search");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleTradingStart(long chatId) {
        AutoTradingEngine engine = AutoTradingEngine.getInstance();
        engine.setBot(this); // Устанавливаем бота для уведомлений
        engine.addNotificationSubscriber(chatId); // Подписываем на уведомления
        
        if (engine.isRunning()) {
            String msg = "⚠️ Автоматическая торговля уже запущена!\n\n" +
                "📢 Вы подписаны на уведомления о всех сделках (прибыльных и убыточных).\n\n" +
                "✅ Вы будете получать уведомления о:\n" +
                "• Прибыльных сделках (🎉 или ✅)\n" +
                "• Убыточных сделках (❌)\n" +
                "• Всех закрытых позициях";
            sendMessage(chatId, msg);
            return;
        }
        
        engine.start();
        String msg = "✅ *Автоматическая торговля запущена!*\n\n" +
            "🤖 Бот будет автоматически торговать используя умные алгоритмы.\n" +
            "📊 Статистика обновляется в реальном времени.\n\n" +
            "📢 *ВЫ ПОДПИСАНЫ НА УВЕДОМЛЕНИЯ!*\n\n" +
            "Вы будете получать уведомления о:\n" +
            "✅ Прибыльных сделках (🎉 или ✅)\n" +
            "❌ Убыточных сделках (❌)\n" +
            "📊 Всех закрытых позициях\n\n" +
            "Каждое уведомление содержит:\n" +
            "• Символ монеты\n" +
            "• Прибыль/Убыток в %\n" +
            "• Цену входа и выхода\n" +
            "• Причину закрытия\n" +
            "• Время удержания";
        sendMessage(chatId, msg);
        
        // Обновляем панель торговли
        commandManager.executeCommand("🤖 Авто-торговля", chatId);
    }
    
    private void handleTradingStop(long chatId) {
        AutoTradingEngine engine = AutoTradingEngine.getInstance();
        if (!engine.isRunning()) {
            sendMessage(chatId, "⚠️ Автоматическая торговля не запущена!");
            return;
        }
        
        engine.stop();
        sendMessage(chatId, "⏹️ Автоматическая торговля остановлена.\n\n" +
            "📊 Все открытые сделки будут закрыты при достижении целей.");
        
        // Обновляем панель торговли
        commandManager.executeCommand("🤖 Авто-торговля", chatId);
    }
    
    private void handleTradingNotifications(long chatId) {
        AutoTradingEngine engine = AutoTradingEngine.getInstance();
        engine.setBot(this); // Устанавливаем бота для уведомлений
        engine.addNotificationSubscriber(chatId); // Подписываем на уведомления
        
        sendMessage(chatId, "✅ *Вы подписаны на уведомления!*\n\n" +
            "📢 Теперь вы будете получать уведомления о:\n" +
            "✅ Прибыльных сделках\n" +
            "❌ Убыточных сделках\n" +
            "📊 Всех закрытых позициях\n\n" +
            "Каждое уведомление содержит:\n" +
            "• Символ монеты\n" +
            "• Прибыль/Убыток в %\n" +
            "• Цену входа и выхода\n" +
            "• Причину закрытия\n" +
            "• Время удержания");
        
        // Обновляем панель торговли
        commandManager.executeCommand("🤖 Авто-торговля", chatId);
    }
    
    private void handleTradingStats(long chatId) {
        TradingManager.TradingStats stats = TradingManager.getStats();
        List<Trade> openTrades = TradingManager.getOpenTrades();
        List<Trade> recentClosedTrades = TradingManager.getClosedTrades();
        
        // Берем последние 10 закрытых сделок
        if (recentClosedTrades.size() > 10) {
            recentClosedTrades = recentClosedTrades.subList(0, 10);
        }
        
        StringBuilder text = new StringBuilder();
        text.append("📊 *Детальная статистика торговли*\n\n");
        text.append("💰 *Баланс:*\n");
        text.append("   Общий: ").append(String.format("%.2f", stats.getTotalBalance())).append(" USDT\n");
        text.append("   Доступно: ").append(String.format("%.2f", stats.getAvailableBalance())).append(" USDT\n\n");
        
        text.append("📈 *Сделки:*\n");
        text.append("   Всего: ").append(stats.getTotalTrades()).append("\n");
        text.append("   Прибыльных: ").append(stats.getProfitableTrades()).append(" (").append(String.format("%.1f", stats.getWinRate())).append("%)\n");
        text.append("   Убыточных: ").append(stats.getLosingTrades()).append("\n");
        text.append("   Открытых: ").append(openTrades.size()).append("\n\n");
        
        text.append("💵 *Прибыльность:*\n");
        text.append("   Общая прибыль: ").append(String.format("%.2f", stats.getTotalProfit())).append("%\n");
        text.append("   Средняя прибыль: ").append(String.format("%.2f", stats.getAvgProfit())).append("%\n\n");
        
        if (!openTrades.isEmpty()) {
            text.append("🔄 *Открытые сделки:*\n");
            for (Trade trade : openTrades) {
                Double currentPriceObj = com.example.telegrambot.trading.PriceService.getPrice(trade.getSymbol());
                double profit = 0.0;
                if (currentPriceObj != null && trade.getType() == Trade.TradeType.BUY) {
                    double currentPrice = currentPriceObj;
                    profit = ((currentPrice - trade.getEntryPrice()) / trade.getEntryPrice()) * 100;
                }
                text.append("   ").append(trade.getType()).append(" ").append(trade.getSymbol())
                    .append(" @ ").append(String.format("%.2f", trade.getEntryPrice()))
                    .append(" (").append(String.format("%.2f", profit)).append("%)\n");
            }
            text.append("\n");
        }
        
        if (!recentClosedTrades.isEmpty()) {
            text.append("📋 *Последние закрытые сделки:*\n");
            for (Trade trade : recentClosedTrades) {
                String emoji = trade.getProfit() > 0 ? "✅" : "❌";
                text.append("   ").append(emoji).append(" ").append(trade.getType())
                    .append(" ").append(trade.getSymbol())
                    .append(" @ ").append(String.format("%.2f", trade.getEntryPrice()))
                    .append(" → ").append(String.format("%.2f", trade.getExitPrice()))
                    .append(" (").append(String.format("%.2f", trade.getProfit())).append("%)\n");
            }
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("trading_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleTradingTrades(long chatId) {
        List<Trade> allTrades = new ArrayList<>();
        allTrades.addAll(TradingManager.getOpenTrades());
        allTrades.addAll(TradingManager.getClosedTrades());
        
        if (allTrades.isEmpty()) {
            sendMessage(chatId, "📋 Сделок пока нет.");
            return;
        }
        
        // Сортируем по времени (новые первые)
        allTrades.sort((t1, t2) -> t2.getEntryTime().compareTo(t1.getEntryTime()));
        
        StringBuilder text = new StringBuilder();
        text.append("📋 *Все сделки*\n\n");
        
        int count = 0;
        for (Trade trade : allTrades) {
            if (count >= 20) break; // Ограничиваем 20 сделками
            
            String statusEmoji = trade.isOpen() ? "🔄" : (trade.getProfit() > 0 ? "✅" : "❌");
            text.append(statusEmoji).append(" ").append(trade.getType())
                .append(" ").append(trade.getSymbol())
                .append(" @ ").append(String.format("%.2f", trade.getEntryPrice()));
            
            if (!trade.isOpen()) {
                text.append(" → ").append(String.format("%.2f", trade.getExitPrice()))
                    .append(" (").append(String.format("%.2f", trade.getProfit())).append("%)");
            } else {
                Double currentPriceObj = com.example.telegrambot.trading.PriceService.getPrice(trade.getSymbol());
                if (currentPriceObj != null) {
                    double currentPrice = currentPriceObj;
                    double profit = ((currentPrice - trade.getEntryPrice()) / trade.getEntryPrice()) * 100;
                    text.append(" (текущая: ").append(String.format("%.2f", currentPrice))
                        .append(", ").append(String.format("%.2f", profit)).append("%)");
                }
            }
            text.append("\n");
            count++;
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("trading_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleTradingStrategies(long chatId) {
        AutoTradingEngine engine = AutoTradingEngine.getInstance();
        List<TradingStrategy> strategies = engine.getStrategies();
        List<String> symbols = engine.getSymbols();
        
        StringBuilder text = new StringBuilder();
        text.append("🧠 *Торговые стратегии*\n\n");
        
        for (TradingStrategy strategy : strategies) {
            text.append("📊 *").append(strategy.getName()).append("*\n");
            text.append(strategy.getDescription()).append("\n\n");
        }
        
        text.append("📈 *Торговые пары:*\n");
        for (String symbol : symbols) {
            text.append("   • ").append(symbol).append("\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("trading_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    // Хранилище для временных данных пополнения/вывода
    private final Map<Long, Double> pendingDepositAmounts = new HashMap<>();
    private final Map<Long, Double> pendingWithdrawAmounts = new HashMap<>();
    
    private void handleWalletDeposit(long chatId) {
        StringBuilder text = new StringBuilder();
        text.append("💳 *Пополнение баланса*\n\n");
        text.append("Выберите сумму для пополнения:\n\n");
        text.append("💵 *Быстрые суммы:*\n");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Быстрые суммы
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton btn50 = new InlineKeyboardButton();
        btn50.setText("50 USDT");
        btn50.setCallbackData("deposit_amount_50");
        row1.add(btn50);
        
        InlineKeyboardButton btn100 = new InlineKeyboardButton();
        btn100.setText("100 USDT");
        btn100.setCallbackData("deposit_amount_100");
        row1.add(btn100);
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn500 = new InlineKeyboardButton();
        btn500.setText("500 USDT");
        btn500.setCallbackData("deposit_amount_500");
        row2.add(btn500);
        
        InlineKeyboardButton btn1000 = new InlineKeyboardButton();
        btn1000.setText("1000 USDT");
        btn1000.setCallbackData("deposit_amount_1000");
        row2.add(btn1000);
        keyboard.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton btnCustom = new InlineKeyboardButton();
        btnCustom.setText("💵 Другая сумма");
        btnCustom.setCallbackData("deposit_custom");
        row3.add(btnCustom);
        keyboard.add(row3);
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("wallet_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleDepositAmount(long chatId, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            pendingDepositAmounts.put(chatId, amount);
            
            StringBuilder text = new StringBuilder();
            text.append("💳 *Пополнение баланса*\n\n");
            text.append("💰 Сумма: ").append(String.format("%.2f", amount)).append(" USDT\n\n");
            text.append("Выберите способ пополнения:\n");
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton cryptoBtn = new InlineKeyboardButton();
            cryptoBtn.setText("₿ Криптовалюта");
            cryptoBtn.setCallbackData("deposit_method_CRYPTO");
            row1.add(cryptoBtn);
            keyboard.add(row1);
            
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton cardBtn = new InlineKeyboardButton();
            cardBtn.setText("💳 Банковская карта");
            cardBtn.setCallbackData("deposit_method_CARD");
            row2.add(cardBtn);
            keyboard.add(row2);
            
            List<InlineKeyboardButton> row3 = new ArrayList<>();
            InlineKeyboardButton bankBtn = new InlineKeyboardButton();
            bankBtn.setText("🏦 Банковский перевод");
            bankBtn.setCallbackData("deposit_method_BANK");
            row3.add(bankBtn);
            keyboard.add(row3);
            
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("⬅️ Назад");
            backButton.setCallbackData("wallet_deposit");
            backRow.add(backButton);
            keyboard.add(backRow);
            
            markup.setKeyboard(keyboard);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text.toString());
            message.setParseMode("Markdown");
            message.setReplyMarkup(markup);
            
            execute(message);
        } catch (Exception e) {
            sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }
    
    private void handleDepositMethod(long chatId, String method) {
        Double amount = pendingDepositAmounts.get(chatId);
        if (amount == null) {
            sendMessage(chatId, "❌ Сумма не выбрана. Начните заново.");
            return;
        }
        
        String methodName = "";
        switch (method) {
            case "CRYPTO": methodName = "Криптовалюта"; break;
            case "CARD": methodName = "Банковская карта"; break;
            case "BANK": methodName = "Банковский перевод"; break;
            default: methodName = method;
        }
        
        WalletService.DepositResult result = WalletService.deposit(chatId, amount, method);
        
        if (result.success) {
            StringBuilder text = new StringBuilder();
            text.append("✅ *Пополнение инициировано*\n\n");
            text.append("💰 Сумма: ").append(String.format("%.2f", amount)).append(" USDT\n");
            text.append("💳 Способ: ").append(methodName).append("\n");
            text.append("🆔 ID транзакции: ").append(result.transactionId != null ? result.transactionId : "N/A").append("\n\n");
            
            if (result.paymentLink != null) {
                text.append("🔗 *Ссылка для оплаты:*\n");
                
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                
                List<InlineKeyboardButton> linkRow = new ArrayList<>();
                InlineKeyboardButton linkButton = new InlineKeyboardButton();
                linkButton.setText("💳 Перейти к оплате");
                linkButton.setUrl(result.paymentLink);
                linkRow.add(linkButton);
                keyboard.add(linkRow);
                
                List<InlineKeyboardButton> backRow = new ArrayList<>();
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("⬅️ Назад в кошелек");
                backButton.setCallbackData("wallet_back");
                backRow.add(backButton);
                keyboard.add(backRow);
                
                markup.setKeyboard(keyboard);
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(text.toString());
                message.setParseMode("Markdown");
                message.setReplyMarkup(markup);
                
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                text.append("✅ Баланс пополнен!\n");
                text.append("💵 Новый баланс: ").append(String.format("%.2f", result.newBalance)).append(" USDT");
                sendMessage(chatId, text.toString());
            }
            
            pendingDepositAmounts.remove(chatId);
        } else {
            sendMessage(chatId, "❌ Ошибка пополнения: " + result.message);
        }
    }
    
    private void handleWalletWithdraw(long chatId) {
        WalletService.WalletBalance balance = WalletService.getBalance(chatId);
        double available = balance != null ? balance.availableBalance : 0.0;
        
        StringBuilder text = new StringBuilder();
        text.append("💸 *Вывод средств*\n\n");
        text.append("💵 Доступно для вывода: ").append(String.format("%.2f", available)).append(" USDT\n\n");
        text.append("Выберите сумму для вывода:\n");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        if (available >= 50) {
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton btn50 = new InlineKeyboardButton();
            btn50.setText("50 USDT");
            btn50.setCallbackData("withdraw_amount_50");
            row1.add(btn50);
            
            if (available >= 100) {
                InlineKeyboardButton btn100 = new InlineKeyboardButton();
                btn100.setText("100 USDT");
                btn100.setCallbackData("withdraw_amount_100");
                row1.add(btn100);
            }
            keyboard.add(row1);
        }
        
        if (available >= 500) {
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton btn500 = new InlineKeyboardButton();
            btn500.setText("500 USDT");
            btn500.setCallbackData("withdraw_amount_500");
            row2.add(btn500);
            
            if (available >= 1000) {
                InlineKeyboardButton btn1000 = new InlineKeyboardButton();
                btn1000.setText("1000 USDT");
                btn1000.setCallbackData("withdraw_amount_1000");
                row2.add(btn1000);
            }
            keyboard.add(row2);
        }
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton btnAll = new InlineKeyboardButton();
        btnAll.setText("💵 Вывести все");
        btnAll.setCallbackData("withdraw_amount_ALL");
        row3.add(btnAll);
        keyboard.add(row3);
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("wallet_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleWithdrawAmount(long chatId, String amountStr) {
        WalletService.WalletBalance balance = WalletService.getBalance(chatId);
        double available = balance != null ? balance.availableBalance : 0.0;
        
        double amount;
        if (amountStr.equals("ALL")) {
            amount = available;
        } else {
            amount = Double.parseDouble(amountStr);
        }
        
        if (amount > available) {
            sendMessage(chatId, "❌ Недостаточно средств. Доступно: " + String.format("%.2f", available) + " USDT");
            return;
        }
        
        if (amount < 10) {
            sendMessage(chatId, "❌ Минимальная сумма вывода: 10 USDT");
            return;
        }
        
        pendingWithdrawAmounts.put(chatId, amount);
        
        sendMessage(chatId, "💸 *Вывод средств*\n\n" +
            "💰 Сумма: " + String.format("%.2f", amount) + " USDT\n\n" +
            "📝 Пожалуйста, отправьте адрес кошелька для вывода.\n" +
            "Формат: просто отправьте адрес в следующем сообщении.");
    }
    
    private void handleWalletHistory(long chatId) {
        com.fasterxml.jackson.databind.JsonNode history = WalletService.getTransactionHistory(chatId, 20);
        
        StringBuilder text = new StringBuilder();
        text.append("📋 *История транзакций*\n\n");
        
        if (history != null && history.has("data") && history.get("data").isArray()) {
            com.fasterxml.jackson.databind.JsonNode transactions = history.get("data");
            if (transactions.size() == 0) {
                text.append("📭 Транзакций пока нет");
            } else {
                for (com.fasterxml.jackson.databind.JsonNode tx : transactions) {
                    String type = tx.has("type") ? tx.get("type").asText() : "UNKNOWN";
                    double amount = tx.has("amount") ? tx.get("amount").asDouble() : 0.0;
                    String status = tx.has("status") ? tx.get("status").asText() : "PENDING";
                    String date = tx.has("date") ? tx.get("date").asText() : "";
                    
                    String emoji = type.equals("DEPOSIT") ? "💳" : "💸";
                    String statusEmoji = status.equals("COMPLETED") ? "✅" : 
                                       status.equals("PENDING") ? "⏳" : "❌";
                    
                    text.append(emoji).append(" ").append(type).append(": ")
                        .append(String.format("%.2f", amount)).append(" USDT ")
                        .append(statusEmoji).append(" ").append(status);
                    if (!date.isEmpty()) {
                        text.append("\n   📅 ").append(date);
                    }
                    text.append("\n\n");
                }
            }
        } else {
            text.append("📭 История загружается...\n");
            text.append("(Используется Back-end API)");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("wallet_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleTradeCoin(long chatId, String symbol) {
        NewCoinScanner.CoinAnalysis analysis = NewCoinScanner.quickAnalyze(symbol);
        
        if (!analysis.shouldTrade()) {
            sendMessage(chatId, "❌ Монета " + symbol + " не рекомендуется для торговли:\n" + analysis.reason);
            return;
        }
        
        Double currentPrice = PriceService.getPrice(symbol);
        if (currentPrice == null) {
            sendMessage(chatId, "❌ Не удалось получить цену для " + symbol);
            return;
        }
        
        double balance = TradingManager.getAvailableBalance(chatId);
        double amount = Math.min(balance * 0.25 / currentPrice, balance / currentPrice * 0.3);
        
        if (amount * currentPrice < 10) {
            sendMessage(chatId, "❌ Недостаточно средств для торговли " + symbol);
            return;
        }
        
        // Используем NewCoinStrategy для принятия решения
        com.example.telegrambot.trading.strategies.NewCoinStrategy strategy = 
            new com.example.telegrambot.trading.strategies.NewCoinStrategy();
        Map<Long, Double> history = PriceService.getPriceHistory(symbol, 15);
        
        TradingDecision decision = strategy.makeDecision(symbol, currentPrice, history, balance);
        
        if (decision.getAction() == TradingDecision.Action.BUY && decision.shouldExecute()) {
            Trade trade = TradingManager.openTrade(symbol, decision);
            if (trade != null) {
                sendMessage(chatId, "✅ *Сделка открыта!*\n\n" +
                    "🪙 Монета: " + symbol + "\n" +
                    "💰 Сумма: " + String.format("%.2f", amount * currentPrice) + " USDT\n" +
                    "🎯 Уверенность: " + String.format("%.1f", decision.getConfidence() * 100) + "%\n" +
                    "📊 Потенциал: " + String.format("%.1f", analysis.potential * 100) + "%\n\n" +
                    "🚀 Система автоматически закроет сделку при достижении целей");
            } else {
                sendMessage(chatId, "❌ Не удалось открыть сделку. Проверьте баланс.");
            }
        } else {
            sendMessage(chatId, "⏳ " + decision.getReason() + "\n\nПопробуйте позже или выберите другую монету.");
        }
    }
    
    private void handleNewCoins(long chatId) {
        StringBuilder text = new StringBuilder();
        text.append("🆕 *Новые монеты*\n");
        text.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        text.append("🔍 *Сканирование новых листингов*\n\n");
        text.append("✨ *Возможности:*\n");
        text.append("• Автоматическое обнаружение новых монет\n");
        text.append("• Быстрая проверка на скам\n");
        text.append("• Умный анализ потенциала\n");
        text.append("• Автоматический вход в перспективные\n\n");
        text.append("⚡ *Система автоматически:*\n");
        text.append("✅ Сканирует новые листинги\n");
        text.append("✅ Проверяет ликвидность и объем\n");
        text.append("✅ Фильтрует скам монеты\n");
        text.append("✅ Входит в перспективные\n");
        text.append("✅ Быстро фиксирует прибыль\n");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton scanBtn = new InlineKeyboardButton();
        scanBtn.setText("🔍 Сканировать сейчас");
        scanBtn.setCallbackData("scan_new_coins_now");
        row1.add(scanBtn);
        keyboard.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton enableBtn = new InlineKeyboardButton();
        enableBtn.setText("✅ Включить авто-сканирование");
        enableBtn.setCallbackData("enable_auto_scan");
        row2.add(enableBtn);
        keyboard.add(row2);
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("cabinet_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleScanNewCoins(long chatId) {
        sendMessage(chatId, "🔍 Сканирование новых монет...\n\n⏳ Пожалуйста, подождите...");
        
        // Сканируем новые монеты
        List<NewCoinScanner.NewCoin> newCoins = NewCoinScanner.scanNewCoins();
        
        StringBuilder text = new StringBuilder();
        text.append("🆕 *Результаты сканирования*\n");
        text.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        if (newCoins.isEmpty()) {
            text.append("📭 Новых монет не найдено\n\n");
            text.append("💡 Система продолжит мониторинг автоматически");
        } else {
            text.append("✅ Найдено новых монет: *").append(newCoins.size()).append("*\n\n");
            
            int count = 0;
            for (NewCoinScanner.NewCoin coin : newCoins) {
                if (count >= 10) break; // Ограничиваем 10 монетами
                
                // Быстрый анализ
                NewCoinScanner.CoinAnalysis analysis = NewCoinScanner.quickAnalyze(coin.symbol);
                
                String status = analysis.isScam ? "❌ Скам" : 
                               analysis.shouldTrade() ? "✅ Перспективная" : "⚠️ Проверка";
                
                text.append(status).append(" *").append(coin.symbol).append("*\n");
                if (!coin.name.isEmpty()) {
                    text.append("   📛 ").append(coin.name).append("\n");
                }
                text.append("   💰 Потенциал: ").append(String.format("%.1f", analysis.potential * 100)).append("%\n");
                text.append("   🎯 Уверенность: ").append(String.format("%.1f", analysis.confidence * 100)).append("%\n");
                
                if (analysis.shouldTrade()) {
                    text.append("   🚀 *Готова к торговле*\n");
                }
                text.append("\n");
                count++;
            }
            
            if (newCoins.size() > 10) {
                text.append("... и еще ").append(newCoins.size() - 10).append(" монет\n");
            }
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton rescanBtn = new InlineKeyboardButton();
        rescanBtn.setText("🔄 Сканировать снова");
        rescanBtn.setCallbackData("scan_new_coins_now");
        row1.add(rescanBtn);
        keyboard.add(row1);
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("cabinet_new_coins");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAnalyzeCoin(long chatId, String symbol) {
        sendMessage(chatId, "🔍 Анализ монеты " + symbol + "...");
        
        NewCoinScanner.CoinAnalysis analysis = NewCoinScanner.quickAnalyze(symbol);
        
        StringBuilder text = new StringBuilder();
        text.append("📊 *Анализ монеты*\n");
        text.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        text.append("🪙 *").append(symbol).append("*\n\n");
        
        text.append("📈 *Статус:*\n");
        if (analysis.isScam) {
            text.append("   ❌ *СКАМ МОНЕТА*\n");
        } else if (analysis.isValid) {
            text.append("   ✅ *Валидна для торговли*\n");
        } else {
            text.append("   ⚠️ *Не рекомендуется*\n");
        }
        text.append("\n");
        
        text.append("💰 *Параметры:*\n");
        text.append("   💵 Цена: ").append(String.format("%.8f", analysis.currentPrice)).append("\n");
        text.append("   🎯 Потенциал: *").append(String.format("%.1f", analysis.potential * 100)).append("%*\n");
        text.append("   ✅ Уверенность: *").append(String.format("%.1f", analysis.confidence * 100)).append("%*\n");
        text.append("\n");
        
        if (!analysis.reason.isEmpty()) {
            text.append("📝 *Примечание:*\n");
            text.append("   ").append(analysis.reason).append("\n\n");
        }
        
        if (analysis.shouldTrade()) {
            text.append("🚀 *Рекомендация: ГОТОВА К ТОРГОВЛЕ*\n");
            text.append("   Система может автоматически войти в эту монету");
        } else if (analysis.isScam) {
            text.append("⚠️ *Рекомендация: ИЗБЕГАТЬ*\n");
            text.append("   Эта монета имеет признаки скама");
        } else {
            text.append("⏳ *Рекомендация: ОЖИДАНИЕ*\n");
            text.append("   Недостаточно данных или низкий потенциал");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        if (analysis.shouldTrade()) {
            List<InlineKeyboardButton> tradeRow = new ArrayList<>();
            InlineKeyboardButton tradeBtn = new InlineKeyboardButton();
            tradeBtn.setText("🚀 Торговать");
            tradeBtn.setCallbackData("trade_coin_" + symbol);
            tradeRow.add(tradeBtn);
            keyboard.add(tradeRow);
        }
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("cabinet_new_coins");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleSettings(long chatId) {
        StringBuilder text = new StringBuilder();
        text.append("⚙️ *Настройки*\n");
        text.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        text.append("🔧 *Параметры торговли:*\n\n");
        text.append("📊 *Стратегии:*\n");
        text.append("   ✅ Обнаружение пампов\n");
        text.append("   ✅ Новые монеты\n");
        text.append("   ✅ Максимизация прибыли\n");
        text.append("   ✅ Управление рисками\n\n");
        text.append("⚡ *Автоматические функции:*\n");
        text.append("   ✅ Авто-сканирование новых монет\n");
        text.append("   ✅ Авто-валидация токенов\n");
        text.append("   ✅ Авто-фиксация прибыли\n\n");
        text.append("💡 Все настройки оптимизированы для максимальной прибыли");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("cabinet_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAutoBuyStart(long chatId) {
        DexAutoBuyService service = DexAutoBuyService.getInstance();
        service.setBot(this);
        service.addNotificationSubscriber(chatId); // Подписываем на уведомления
        
        if (service.isRunning()) {
            sendMessage(chatId, "⚠️ Авто-закупка уже запущена!");
            return;
        }
        
        service.start();
        sendMessage(chatId, "✅ *Авто-закупка запущена!*\n\n" +
            "🤖 *Умная стратегия:*\n" +
            "• Отслеживает новые монеты каждую минуту\n" +
            "• Ждет пампов (рост 5%+)\n" +
            "• Быстро входит в лонг при пампе\n" +
            "• Выходит при прибыли 10-15%+\n" +
            "• Отправляет уведомления о всех операциях\n\n" +
            "📢 Вы будете получать уведомления о покупках и продажах!");
        
        // Обновляем панель
        commandManager.executeCommand("🛒 Авто-закупка", chatId);
    }
    
    private void handleAutoBuyStop(long chatId) {
        DexAutoBuyService service = DexAutoBuyService.getInstance();
        
        if (!service.isRunning()) {
            sendMessage(chatId, "⚠️ Авто-закупка не запущена!");
            return;
        }
        
        service.stop();
        sendMessage(chatId, "⏹️ *Авто-закупка остановлена.*\n\n" +
            "Все открытые позиции будут проверяться для продажи.");
        
        // Обновляем панель
        commandManager.executeCommand("🛒 Авто-закупка", chatId);
    }
    
    private void handleAutoBuyStats(long chatId) {
        DexAutoBuyService service = DexAutoBuyService.getInstance();
        
        StringBuilder text = new StringBuilder();
        text.append("📊 *Статистика авто-закупки*\n");
        text.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        if (service.isRunning()) {
            text.append("✅ *Статус:* Включена\n\n");
        } else {
            text.append("⏸️ *Статус:* Выключена\n\n");
        }
        
        text.append("📈 *Информация:*\n");
        text.append("• Сканирование: каждые 3 минуты\n");
        text.append("• Покупка: топ-3 новые монеты\n");
        text.append("• Время удержания: 30 минут\n");
        text.append("• Минимальная ликвидность: 50,000 USDT\n");
        text.append("• Размер позиции: 10% баланса на монету\n\n");
        text.append("💡 Система автоматически валидирует токены и отправляет уведомления");
        
        sendMessage(chatId, text.toString());
    }
}
