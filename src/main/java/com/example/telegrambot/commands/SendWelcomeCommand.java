package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
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
        // АГРЕССИВНОЕ удаление старой клавиатуры - ТРИ РАЗА
        try {
            ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
            removeKeyboard.setRemoveKeyboard(true);
            
            // Первое удаление
            SendMessage removeMsg1 = new SendMessage();
            removeMsg1.setChatId(chatId);
            removeMsg1.setReplyMarkup(removeKeyboard);
            bot.execute(removeMsg1);
            Thread.sleep(200);
            
            // Второе удаление
            SendMessage removeMsg2 = new SendMessage();
            removeMsg2.setChatId(chatId);
            removeMsg2.setText(" ");
            removeMsg2.setReplyMarkup(removeKeyboard);
            bot.execute(removeMsg2);
            Thread.sleep(200);
            
            // Третье удаление
            SendMessage removeMsg3 = new SendMessage();
            removeMsg3.setChatId(chatId);
            removeMsg3.setReplyMarkup(removeKeyboard);
            bot.execute(removeMsg3);
            Thread.sleep(300);
        } catch (Exception e) {
            // Игнорируем
        }
        
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
        
        // Create keyboard с новыми кнопками
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        
        // Отправляем сообщение с новой клавиатурой
        sendMessageWithKeyboard(chatId, text, keyboard);
        
        // ОТПРАВЛЯЕМ ЕЩЕ РАЗ для гарантии
        try {
            Thread.sleep(500);
            SendMessage confirmMsg = new SendMessage();
            confirmMsg.setChatId(chatId);
            confirmMsg.setText("✅ *КНОПКА ДОСТУПНА:* 🤖 Авто-торговля\n\nНажмите на неё для запуска!");
            confirmMsg.setParseMode("Markdown");
            confirmMsg.setReplyMarkup(keyboard);
            bot.execute(confirmMsg);
        } catch (Exception e) {
            // Игнорируем
        }
    }
    
    @Override
    public String getDescription() {
        return "Show welcome message";
    }
}
