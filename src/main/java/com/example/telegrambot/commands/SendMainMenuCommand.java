package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Command for sending main menu
 */
public class SendMainMenuCommand extends BaseCommand {
    
    public SendMainMenuCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🏠 *Main Menu*\n\n" +
                "Select a section:";
        
        // Create inline keyboard
        InlineKeyboardMarkup markup = KeyboardFactory.createMainMenuKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Show main menu";
    }
}
