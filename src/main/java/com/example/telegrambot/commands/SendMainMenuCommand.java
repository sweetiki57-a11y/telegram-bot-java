package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.factory.KeyboardFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Command for sending main menu
 */
public class SendMainMenuCommand extends BaseCommand {
    
    public SendMainMenuCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        // Сначала удаляем старую клавиатуру
        try {
            SendMessage removeMessage = new SendMessage();
            removeMessage.setChatId(chatId);
            ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
            removeKeyboard.setRemoveKeyboard(true);
            removeMessage.setReplyMarkup(removeKeyboard);
            bot.execute(removeMessage);
            Thread.sleep(300);
        } catch (Exception e) {
            // Игнорируем
        }
        
        String text = "🏠 *Главное меню*\n\n" +
                "Выберите раздел:";
        
        // Создаем Reply клавиатуру с новыми кнопками
        ReplyKeyboardMarkup keyboard = KeyboardFactory.createMainKeyboard();
        
        sendMessageWithKeyboard(chatId, text, keyboard);
    }
    
    @Override
    public String getDescription() {
        return "Show main menu";
    }
}
