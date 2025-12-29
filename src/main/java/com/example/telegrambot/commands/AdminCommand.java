package com.example.telegrambot.commands;

import com.example.telegrambot.AdminPanel;
import com.example.telegrambot.MyTelegramBot;

/**
 * Команда для админ панели
 */
public class AdminCommand extends BaseCommand {
    
    public AdminCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        // Используем существующую AdminPanel
        if (AdminPanel.isAdmin(chatId)) {
            AdminPanel.showAdminPanel(bot, chatId);
        } else {
            sendMessage(chatId, "❌ У вас нет прав администратора.");
        }
    }
    
    @Override
    public String getDescription() {
        return "Показать админ панель";
    }
}
