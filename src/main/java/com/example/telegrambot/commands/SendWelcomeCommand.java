package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Command for sending welcome message
 */
public class SendWelcomeCommand extends BaseCommand {
    
    public SendWelcomeCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "🎉 *Добро пожаловать!*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "💰 *Автоматическая торговля криптовалютой*\n\n" +
                "✨ *Возможности:*\n" +
                "🤖 Автоматическая торговля\n" +
                "🚀 Обнаружение пампов\n" +
                "🆕 Торговля новыми монетами\n" +
                "💰 Управление кошельком\n" +
                "🛒 Авто-закупка новых токенов\n" +
                "📊 Детальная статистика\n\n" +
                "👤 *Начните с личного кабинета!*\n\n" +
                "💡 *Быстрый старт:*\n" +
                "1️⃣ Пополните баланс\n" +
                "2️⃣ Запустите торговлю\n" +
                "3️⃣ Получайте прибыль автоматически";
        
        // Create keyboard - принудительно обновляем
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        
        // Отправляем сообщение с клавиатурой
        sendMessageWithKeyboard(chatId, text, keyboard);
    }
    
    @Override
    public String getDescription() {
        return "Show welcome message";
    }
}
