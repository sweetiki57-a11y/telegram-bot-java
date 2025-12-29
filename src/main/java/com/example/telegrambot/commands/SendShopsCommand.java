package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Command for sending shops list
 */
public class SendShopsCommand extends BaseCommand {
    
    public SendShopsCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🛒 Here is a list of our trusted stores where you can shop with confidence!";
        
        // Create inline keyboard with link buttons
        InlineKeyboardMarkup markup = KeyboardFactory.createShopsKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Show list of all shops";
    }
}
