package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Command for sending top shops
 */
public class SendTopCommand extends BaseCommand {
    
    public SendTopCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🏆 Top-5 Best Shops Today:\n\n" +
                      "Select a store from the list below:";
        
        // Create inline keyboard with top-5 shops
        InlineKeyboardMarkup markup = KeyboardFactory.createTopShopsKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Show top-5 best shops";
    }
}

