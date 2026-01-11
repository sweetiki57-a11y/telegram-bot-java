package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.trading.DeepCoinAnalyzer;
import com.example.telegrambot.trading.NewCoinScanner;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для отображения листинга перспективных монет
 */
public class SendCoinListingCommand extends BaseCommand {
    
    public SendCoinListingCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        try {
            bot.sendMessage(chatId, "🔍 *Анализ перспективных монет...*\n\nПодождите, собираю данные...");
            
            // Получаем новые монеты
            List<NewCoinScanner.NewCoin> newCoins = NewCoinScanner.scanNewCoins();
            
            if (newCoins.isEmpty()) {
                bot.sendMessage(chatId, "⚠️ Новые монеты не найдены. Попробуйте позже.");
                return;
            }
            
            // Анализируем каждую монету
            List<DeepCoinAnalyzer.DeepAnalysis> promisingCoins = new ArrayList<>();
            
            for (NewCoinScanner.NewCoin coin : newCoins) {
                DeepCoinAnalyzer.DeepAnalysis analysis = DeepCoinAnalyzer.analyzeToken(coin.symbol);
                if (analysis.isPromising && analysis.error == null) {
                    promisingCoins.add(analysis);
                }
                
                // Ограничиваем количество для производительности
                if (promisingCoins.size() >= 20) break;
            }
            
            // Сортируем по скору
            promisingCoins.sort((a, b) -> Double.compare(b.score, a.score));
            
            // Формируем сообщение
            StringBuilder message = new StringBuilder();
            message.append("📊 *ЛИСТИНГ ПЕРСПЕКТИВНЫХ МОНЕТ*\n");
            message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            
            if (promisingCoins.isEmpty()) {
                message.append("⚠️ Перспективные монеты не найдены.\n");
                message.append("Попробуйте позже или проверьте другие токены.");
            } else {
                message.append("✅ Найдено: *").append(promisingCoins.size()).append("* перспективных монет\n\n");
                
                int count = 0;
                for (DeepCoinAnalyzer.DeepAnalysis analysis : promisingCoins) {
                    if (count >= 10) break; // Показываем топ 10
                    
                    String emoji = analysis.score >= 80 ? "🚀" : analysis.score >= 70 ? "✅" : "⚠️";
                    message.append(emoji).append(" *").append(analysis.symbol).append("*\n");
                    message.append("   💰 Цена: $").append(String.format("%.8f", analysis.currentPrice)).append("\n");
                    message.append("   📈 24ч: ").append(String.format("%.2f", analysis.priceChange24h)).append("%\n");
                    message.append("   💧 Ликвидность: $").append(String.format("%.0f", analysis.liquidity)).append("\n");
                    message.append("   ⭐ Скор: ").append(String.format("%.0f", analysis.score)).append("/100\n");
                    message.append("   🎯 ").append(analysis.recommendation).append("\n\n");
                    
                    count++;
                }
            }
            
            message.append("🔄 *Обновляется в реальном времени*\n");
            message.append("Нажмите кнопку ниже для обновления");
            
            // Создаем клавиатуру
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            List<InlineKeyboardButton> refreshRow = new ArrayList<>();
            InlineKeyboardButton refreshBtn = new InlineKeyboardButton();
            refreshBtn.setText("🔄 Обновить листинг");
            refreshBtn.setCallbackData("refresh_coin_listing");
            refreshRow.add(refreshBtn);
            keyboard.add(refreshRow);
            
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backBtn = new InlineKeyboardButton();
            backBtn.setText("⬅️ Назад");
            backBtn.setCallbackData("back_to_main_menu");
            backRow.add(backBtn);
            keyboard.add(backRow);
            
            markup.setKeyboard(keyboard);
            
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText(message.toString());
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(markup);
            
            bot.execute(msg);
            
        } catch (Exception e) {
            bot.sendMessage(chatId, "❌ Ошибка при получении листинга: " + e.getMessage());
        }
    }
    
    @Override
    public String getDescription() {
        return "Листинг перспективных монет";
    }
}
