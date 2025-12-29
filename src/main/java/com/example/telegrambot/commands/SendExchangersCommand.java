package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Команда для отправки списка обменников
 */
public class SendExchangersCommand extends BaseCommand {
    
    public SendExchangersCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "💰 *Обменники*\n\n" +
                "🔄 *Доступные обменники:*\n\n" +
                "Выберите обменник для перехода:";
        
        // Создаем inline клавиатуру с кнопками-ссылками
        InlineKeyboardMarkup markup = KeyboardFactory.createExchangersKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Показать список всех обменников";
    }
}
