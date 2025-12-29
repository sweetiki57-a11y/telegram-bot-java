package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;

/**
 * Command for sending help information
 */
public class SendHelpCommand extends BaseCommand {
    
    public SendHelpCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "❓ *Bot Help Guide*\n\n" +
                "🤖 *Main Commands:*\n" +
                "/start - Start the bot\n" +
                "/menu - Main menu\n" +
                "/help - This help\n" +
                "/admin - Admin panel\n\n" +
                "🛒 *Shops:*\n" +
                "Shows list of all available stores with direct links\n\n" +
                "💰 *Exchangers:*\n" +
                "List of verified exchangers for secure operations\n\n" +
                "🔍 *Category Search:*\n" +
                "Use the '🔍 Category Search' button to search channels by emoji\n\n" +
                "📄 *Header:*\n" +
                "Complete list of all channels and contacts\n\n" +
                "🏆 *Top:*\n" +
                "Top-5 randomly selected stores of this week\n\n" +
                "📞 *Support:*\n" +
                "If you have questions, contact the administrator";
        
        sendMessage(chatId, text);
    }
    
    @Override
    public String getDescription() {
        return "Show bot help guide";
    }
}
