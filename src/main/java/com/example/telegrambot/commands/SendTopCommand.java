package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Команда для отправки топа шопов
 */
public class SendTopCommand extends BaseCommand {
    
    public SendTopCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🏆 Топ-5 лучших шопов на сегодня:\n\n" +
                      "Выберите магазин из списка ниже:";
        
        // Создаем inline клавиатуру с топ-5 шопов
        InlineKeyboardMarkup markup = KeyboardFactory.createTopShopsKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Показать топ-5 лучших шопов";
    }
}

