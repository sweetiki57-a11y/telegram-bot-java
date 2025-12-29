package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Command for sending category search prompt
 */
public class SendSearchPromptCommand extends BaseCommand {
    
    public SendSearchPromptCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🔍 Select a category to search:";
        
        // Create inline keyboard with categories
        InlineKeyboardMarkup markup = KeyboardFactory.createSearchCategoriesKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Search products by categories";
    }
}

