package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Command for sending exchangers list
 */
public class SendExchangersCommand extends BaseCommand {
    
    public SendExchangersCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "💰 *Exchangers*\n\n" +
                "🔄 *Available Exchangers:*\n\n" +
                "Select an exchanger to proceed:";
        
        // Create inline keyboard with link buttons
        InlineKeyboardMarkup markup = KeyboardFactory.createExchangersKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Show list of all exchangers";
    }
}
