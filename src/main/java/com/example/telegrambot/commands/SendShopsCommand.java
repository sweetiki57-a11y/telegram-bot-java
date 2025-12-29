package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Команда для отправки списка шопов
 */
public class SendShopsCommand extends BaseCommand {
    
    public SendShopsCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🛒 Вот список наших надежных магазинов, где вы можете делать покупки без забот!";
        
        // Создаем inline клавиатуру с кнопками-ссылками
        InlineKeyboardMarkup markup = KeyboardFactory.createShopsKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Показать список всех шопов";
    }
}
