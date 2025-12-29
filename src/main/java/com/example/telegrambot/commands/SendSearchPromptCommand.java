package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Команда для отправки запроса на поиск по категориям
 */
public class SendSearchPromptCommand extends BaseCommand {
    
    public SendSearchPromptCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🔍 Выберите категорию по которой будет производиться поиск:";
        
        // Создаем inline клавиатуру с категориями
        InlineKeyboardMarkup markup = KeyboardFactory.createSearchCategoriesKeyboard();
        
        sendMessageWithKeyboard(chatId, text, markup);
    }
    
    @Override
    public String getDescription() {
        return "Поиск товаров по категориям";
    }
}

