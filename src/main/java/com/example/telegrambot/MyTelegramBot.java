package com.example.telegrambot;

import com.example.telegrambot.commands.CommandManager;
import com.example.telegrambot.factory.KeyboardFactory;
import com.example.telegrambot.payment.PaymentMethod;
import com.example.telegrambot.payment.PaymentMethodFactory;
import com.example.telegrambot.payment.PaymentProcessor;
import com.example.telegrambot.payment.PaymentResult;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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
        String welcomeText = "🎉 *Welcome to Fredo Store!*\n\n" +
                "🛍️ *Your Online Store*\n\n" +
                "🛒 *How to Order:*\n" +
                "1️⃣ Choose a product\n" +
                "2️⃣ Add to cart\n" +
                "3️⃣ Checkout\n\n" +
                "Use the buttons below! 🛍️";
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(welcomeText);
        message.setParseMode("Markdown");
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        message.setReplyMarkup(keyboard);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
}
