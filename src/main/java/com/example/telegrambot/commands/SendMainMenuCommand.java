package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Команда для отправки главного меню
 */
public class SendMainMenuCommand extends BaseCommand {
    
    public SendMainMenuCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🏠 *Главное меню*\n\n" +
                "Выберите нужный раздел:";
        
        // Создаем inline клавиатуру
        InlineKeyboardMarkup markup = KeyboardFactory.createMainMenuKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Показать главное меню";
    }
}
