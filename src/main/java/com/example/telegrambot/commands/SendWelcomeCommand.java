package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

/**
 * Command for sending welcome message
 */
public class SendWelcomeCommand extends BaseCommand {
    
    public SendWelcomeCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "👽 *Welcome to Inoplanetane!* 👽\n\n" +
                "🚀 *Welcome to Inoplanetane!* 🚀\n\n" +
                "We are your reliable guide to quality products and services!\n\n" +
                "✨ *What we offer:*\n" +
                "• 🛒 Shops - verified stores\n" +
                "• 💰 Exchangers - secure exchanges\n" +
                "• 🔍 Category Search - quick search\n" +
                "• 📄 Header - all channels and contacts\n" +
                "• 🏆 Top - best stores\n\n" +
                "Use menu buttons for navigation!";
        
        // Create keyboard
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        
        sendMessageWithKeyboard(chatId, text, keyboard);
    }
    
    @Override
    public String getDescription() {
        return "Show welcome message";
    }
}
