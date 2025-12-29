package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Базовый класс для всех команд
 * Содержит общие методы для работы с ботом
 */
public abstract class BaseCommand implements Command {
    protected final MyTelegramBot bot;
    
    public BaseCommand(MyTelegramBot bot) {
        this.bot = bot;
    }
    
    /**
     * Отправляет сообщение в чат
     * @param chatId ID чата
     * @param text текст сообщения
     */
    protected void sendMessage(long chatId, String text) {
        try {
            bot.sendMessage(chatId, text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Отправляет сообщение с клавиатурой
     * @param chatId ID чата
     * @param text текст сообщения
     * @param keyboard клавиатура
     */
    protected void sendMessageWithKeyboard(long chatId, String text, Object keyboard) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            
            if (keyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) {
                message.setReplyMarkup((org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) keyboard);
            } else if (keyboard instanceof org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) {
                message.setReplyMarkup((org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) keyboard);
            }
            
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

