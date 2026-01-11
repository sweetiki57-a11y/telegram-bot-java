package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Base class for all commands
 * Contains common methods for working with the bot
 */
public abstract class BaseCommand implements Command {
    protected final MyTelegramBot bot;
    
    public BaseCommand(MyTelegramBot bot) {
        this.bot = bot;
    }
    
    /**
     * Sends message to chat
     * @param chatId chat ID
     * @param text message text
     */
    protected void sendMessage(long chatId, String text) {
        try {
            bot.sendMessage(chatId, text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends message with keyboard
     * @param chatId chat ID
     * @param text message text
     * @param keyboard keyboard
     */
    protected void sendMessageWithKeyboard(long chatId, String text, Object keyboard) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setParseMode("Markdown");
            
            if (keyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) {
                message.setReplyMarkup((org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) keyboard);
            } else if (keyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
                ReplyKeyboardMarkup replyKeyboard = (org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) keyboard;
                // Принудительно обновляем клавиатуру
                replyKeyboard.setOneTimeKeyboard(true); // Временно true для обновления
                message.setReplyMarkup(replyKeyboard);
            }
            
            bot.execute(message);
            
            // Если это ReplyKeyboard, отправляем еще одно сообщение с постоянной клавиатурой
            if (keyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
                ReplyKeyboardMarkup replyKeyboard = (org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) keyboard;
                replyKeyboard.setOneTimeKeyboard(false); // Возвращаем false для постоянной клавиатуры
                SendMessage updateMessage = new SendMessage();
                updateMessage.setChatId(chatId);
                updateMessage.setText("📱 *Меню обновлено!*\n\nИспользуйте кнопки ниже:");
                updateMessage.setParseMode("Markdown");
                updateMessage.setReplyMarkup(replyKeyboard);
                bot.execute(updateMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

