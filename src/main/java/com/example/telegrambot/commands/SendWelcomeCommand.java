package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

/**
 * Команда для отправки приветственного сообщения
 */
public class SendWelcomeCommand extends BaseCommand {
    
    public SendWelcomeCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "👽 *Добро пожаловать в Inoplanetane!* 👽\n\n" +
                "🚀 *Добро пожаловать в Inoplanetane!* 🚀\n\n" +
                "Мы - ваш надежный проводник в мир качественных товаров и услуг!\n\n" +
                "✨ *Что мы предлагаем:*\n" +
                "• 🛒 Шопы - проверенные магазины\n" +
                "• 💰 Обменники - безопасные обмены\n" +
                "• 🔍 Поиск по категориям - быстрый поиск\n" +
                "• 📄 Шапка - все каналы и контакты\n" +
                "• 🏆 Топ - лучшие магазины\n\n" +
                "Используйте кнопки меню для навигации!";
        
        // Создаем клавиатуру
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        
        sendMessageWithKeyboard(chatId, text, keyboard);
    }
    
    @Override
    public String getDescription() {
        return "Показать приветственное сообщение";
    }
}
