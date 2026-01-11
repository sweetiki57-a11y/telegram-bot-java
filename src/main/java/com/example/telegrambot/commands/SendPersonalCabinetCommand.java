package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.trading.AutoTradingEngine;
import com.example.telegrambot.trading.TradingManager;
import com.example.telegrambot.trading.WalletService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для красивого личного кабинета
 */
public class SendPersonalCabinetCommand extends BaseCommand {
    
    public SendPersonalCabinetCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        WalletService.WalletBalance balance = WalletService.getBalance(chatId);
        TradingManager.TradingStats stats = TradingManager.getStats();
        AutoTradingEngine engine = AutoTradingEngine.getInstance();
        
        StringBuilder messageText = new StringBuilder();
        messageText.append("👤 *Личный кабинет*\n");
        messageText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        // Баланс
        messageText.append("💰 *Баланс:*\n");
        if (balance != null) {
            messageText.append("   💵 Общий: *").append(String.format("%.2f", balance.totalBalance))
                      .append("* ").append(balance.currency).append("\n");
            messageText.append("   ✅ Доступно: *").append(String.format("%.2f", balance.availableBalance))
                      .append("* ").append(balance.currency).append("\n");
            if (balance.lockedBalance > 0) {
                messageText.append("   🔒 В сделках: *").append(String.format("%.2f", balance.lockedBalance))
                          .append("* ").append(balance.currency).append("\n");
            }
        } else {
            messageText.append("   💵 Загрузка...\n");
        }
        messageText.append("\n");
        
        // Статистика торговли
        messageText.append("📊 *Торговля:*\n");
        messageText.append("   🤖 Статус: ").append(engine.isRunning() ? "*✅ Активна*" : "*⏸ Остановлена*").append("\n");
        messageText.append("   📈 Сделок: *").append(stats.getTotalTrades()).append("*\n");
        messageText.append("   ✅ Прибыльных: *").append(stats.getProfitableTrades()).append("* (")
                  .append(String.format("%.1f", stats.getWinRate())).append("%)\n");
        messageText.append("   💵 Прибыль: *").append(String.format("%.2f", stats.getTotalProfit())).append("%*\n");
        messageText.append("\n");
        
        // Быстрые действия
        messageText.append("⚡ *Быстрые действия:*\n");
        messageText.append("   • Пополнить баланс\n");
        messageText.append("   • Вывести средства\n");
        messageText.append("   • Управление торговлей\n");
        messageText.append("   • История операций\n");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Первая строка - основные действия
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton depositBtn = new InlineKeyboardButton();
        depositBtn.setText("💳 Пополнить");
        depositBtn.setCallbackData("wallet_deposit");
        row1.add(depositBtn);
        
        InlineKeyboardButton withdrawBtn = new InlineKeyboardButton();
        withdrawBtn.setText("💸 Вывести");
        withdrawBtn.setCallbackData("wallet_withdraw");
        row1.add(withdrawBtn);
        keyboard.add(row1);
        
        // Вторая строка - торговля
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton tradingBtn = new InlineKeyboardButton();
        tradingBtn.setText("🤖 Торговля");
        tradingBtn.setCallbackData("cabinet_trading");
        row2.add(tradingBtn);
        
        InlineKeyboardButton newCoinsBtn = new InlineKeyboardButton();
        newCoinsBtn.setText("🆕 Новые монеты");
        newCoinsBtn.setCallbackData("cabinet_new_coins");
        row2.add(newCoinsBtn);
        keyboard.add(row2);
        
        // Третья строка - статистика и история
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton statsBtn = new InlineKeyboardButton();
        statsBtn.setText("📊 Статистика");
        statsBtn.setCallbackData("trading_stats");
        row3.add(statsBtn);
        
        InlineKeyboardButton historyBtn = new InlineKeyboardButton();
        historyBtn.setText("📋 История");
        historyBtn.setCallbackData("wallet_history");
        row3.add(historyBtn);
        keyboard.add(row3);
        
        // Четвертая строка - настройки
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton settingsBtn = new InlineKeyboardButton();
        settingsBtn.setText("⚙️ Настройки");
        settingsBtn.setCallbackData("cabinet_settings");
        row4.add(settingsBtn);
        keyboard.add(row4);
        
        // Кнопка обновить
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton refreshBtn = new InlineKeyboardButton();
        refreshBtn.setText("🔄 Обновить");
        refreshBtn.setCallbackData("cabinet_refresh");
        row5.add(refreshBtn);
        keyboard.add(row5);
        
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
        return "Личный кабинет";
    }
}
