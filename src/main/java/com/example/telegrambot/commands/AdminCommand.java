package com.example.telegrambot.commands;

import com.example.telegrambot.AdminPanel;
import com.example.telegrambot.MyTelegramBot;

/**
 * Command for admin panel
 */
public class AdminCommand extends BaseCommand {
    
    public AdminCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        // Use existing AdminPanel
        if (AdminPanel.isAdmin(chatId)) {
            AdminPanel.showAdminPanel(bot, chatId);
        } else {
            sendMessage(chatId, "❌ You don't have administrator rights.");
        }
    }
    
    @Override
    public String getDescription() {
        return "Show admin panel";
    }
}
