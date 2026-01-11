package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.trading.AutoTradingEngine;
import com.example.telegrambot.trading.TradingManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для управления автоматической торговлей
 */
public class SendTradingCommand extends BaseCommand {
    
    public SendTradingCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        AutoTradingEngine engine = AutoTradingEngine.getInstance();
        TradingManager.TradingStats stats = TradingManager.getStats();
        
        StringBuilder messageText = new StringBuilder();
        messageText.append("🤖 *Автоматическая торговля*\n");
        messageText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        if (engine.isRunning()) {
            messageText.append("✅ *Статус:* *Активна*\n");
        } else {
            messageText.append("⏸️ *Статус:* *Остановлена*\n");
        }
        messageText.append("\n");
        
        messageText.append("💰 *Баланс:*\n");
        messageText.append("   💵 Общий: *").append(String.format("%.2f", stats.getTotalBalance())).append("* USDT\n");
        messageText.append("   ✅ Доступно: *").append(String.format("%.2f", stats.getAvailableBalance())).append("* USDT\n");
        messageText.append("\n");
        
        messageText.append("📊 *Статистика:*\n");
        messageText.append("   📈 Сделок: *").append(stats.getTotalTrades()).append("*\n");
        messageText.append("   ✅ Прибыльных: *").append(stats.getProfitableTrades()).append("* (")
                  .append(String.format("%.1f", stats.getWinRate())).append("%)\n");
        messageText.append("   ❌ Убыточных: *").append(stats.getLosingTrades()).append("*\n");
        messageText.append("   💰 Прибыль: *").append(String.format("%.2f", stats.getTotalProfit())).append("%*\n");
        messageText.append("   📈 Средняя: *").append(String.format("%.2f", stats.getAvgProfit())).append("%*\n");
        
        // Открытые сделки
        List<com.example.telegrambot.trading.Trade> openTrades = TradingManager.getOpenTrades();
        if (!openTrades.isEmpty()) {
            messageText.append("\n🔄 *Открытые сделки:* ").append(openTrades.size()).append("\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопки управления
        List<InlineKeyboardButton> controlRow = new ArrayList<>();
        
        if (engine.isRunning()) {
            InlineKeyboardButton stopButton = new InlineKeyboardButton();
            stopButton.setText("⏹️ Остановить");
            stopButton.setCallbackData("trading_stop");
            controlRow.add(stopButton);
        } else {
            InlineKeyboardButton startButton = new InlineKeyboardButton();
            startButton.setText("▶️ Запустить");
            startButton.setCallbackData("trading_start");
            controlRow.add(startButton);
        }
        
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("📊 Детальная статистика");
        statsButton.setCallbackData("trading_stats");
        controlRow.add(statsButton);
        
        keyboard.add(controlRow);
        
        // Кнопки дополнительных действий
        List<InlineKeyboardButton> actionsRow = new ArrayList<>();
        
        InlineKeyboardButton tradesButton = new InlineKeyboardButton();
        tradesButton.setText("📋 Список сделок");
        tradesButton.setCallbackData("trading_trades");
        actionsRow.add(tradesButton);
        
        InlineKeyboardButton strategiesButton = new InlineKeyboardButton();
        strategiesButton.setText("🧠 Стратегии");
        strategiesButton.setCallbackData("trading_strategies");
        actionsRow.add(strategiesButton);
        
        keyboard.add(actionsRow);
        
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
        return "Управление автоматической торговлей";
    }
}
