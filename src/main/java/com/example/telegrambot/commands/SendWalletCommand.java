package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.trading.WalletService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для управления кошельком и реальными деньгами
 */
public class SendWalletCommand extends BaseCommand {
    
    public SendWalletCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        WalletService.WalletBalance balance = WalletService.getBalance(chatId);
        
        StringBuilder messageText = new StringBuilder();
        messageText.append("💰 *Мой кошелек*\n");
        messageText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        if (balance != null) {
            messageText.append("💵 *Баланс:*\n");
            messageText.append("   Общий: ").append(String.format("%.2f", balance.totalBalance)).append(" ").append(balance.currency).append("\n");
            messageText.append("   Доступно: ").append(String.format("%.2f", balance.availableBalance)).append(" ").append(balance.currency).append("\n");
            if (balance.lockedBalance > 0) {
                messageText.append("   В сделках: ").append(String.format("%.2f", balance.lockedBalance)).append(" ").append(balance.currency).append("\n");
            }
        } else {
            messageText.append("💵 *Баланс:* Загрузка...\n");
            messageText.append("   (Используется Back-end API)\n");
        }
        
        messageText.append("\n✨ *Доступные операции:*\n");
        messageText.append("• Пополнить баланс\n");
        messageText.append("• Вывести средства\n");
        messageText.append("• История транзакций\n");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопки операций
        List<InlineKeyboardButton> operationsRow = new ArrayList<>();
        
        InlineKeyboardButton depositButton = new InlineKeyboardButton();
        depositButton.setText("💳 Пополнить");
        depositButton.setCallbackData("wallet_deposit");
        operationsRow.add(depositButton);
        
        InlineKeyboardButton withdrawButton = new InlineKeyboardButton();
        withdrawButton.setText("💸 Вывести");
        withdrawButton.setCallbackData("wallet_withdraw");
        operationsRow.add(withdrawButton);
        
        keyboard.add(operationsRow);
        
        // Кнопка истории
        List<InlineKeyboardButton> historyRow = new ArrayList<>();
        InlineKeyboardButton historyButton = new InlineKeyboardButton();
        historyButton.setText("📋 История транзакций");
        historyButton.setCallbackData("wallet_history");
        historyRow.add(historyButton);
        keyboard.add(historyRow);
        
        // Кнопка обновить
        List<InlineKeyboardButton> refreshRow = new ArrayList<>();
        InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText("🔄 Обновить баланс");
        refreshButton.setCallbackData("wallet_refresh");
        refreshRow.add(refreshButton);
        keyboard.add(refreshRow);
        
        // Кнопка назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("back_to_main_menu");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        markup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText.toString());
        message.setParseMode("Markdown");
        message.setReplyMarkup(markup);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public String getDescription() {
        return "Управление кошельком и реальными деньгами";
    }
}
