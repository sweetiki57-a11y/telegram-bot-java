package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;
import com.example.telegrambot.trading.DexAutoBuyService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для управления автоматической закупкой новых токенов
 */
public class SendAutoBuyCommand extends BaseCommand {
    
    public SendAutoBuyCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        DexAutoBuyService service = DexAutoBuyService.getInstance();
        service.setBot(bot);
        
        StringBuilder messageText = new StringBuilder();
        messageText.append("🛒 *Автоматическая закупка новых токенов*\n");
        messageText.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        if (service.isRunning()) {
            messageText.append("✅ *Статус:* Включена\n\n");
            messageText.append("🤖 *Что делает:*\n");
            messageText.append("• Сканирует топ новых монет на DEX\n");
            messageText.append("• Автоматически покупает топ-3 новые монеты\n");
            messageText.append("• Валидирует токены перед покупкой\n");
            messageText.append("• Держит позиции 30 минут\n");
            messageText.append("• Отправляет уведомления о покупках\n\n");
            messageText.append("⏰ *Интервалы:*\n");
            messageText.append("• Сканирование: каждые 3 минуты\n");
            messageText.append("• Проверка позиций: каждую минуту\n");
        } else {
            messageText.append("⏸️ *Статус:* Выключена\n\n");
            messageText.append("🤖 *Что будет делать:*\n");
            messageText.append("• Сканировать топ новых монет на DEX\n");
            messageText.append("• Автоматически покупать топ-3 новые монеты\n");
            messageText.append("• Валидировать токены перед покупкой\n");
            messageText.append("• Держать позиции 30 минут\n");
            messageText.append("• Отправлять уведомления о покупках\n");
        }
        
        // Создаем кнопки
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка включения/выключения
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton toggleButton = new InlineKeyboardButton();
        if (service.isRunning()) {
            toggleButton.setText("⏹️ Остановить авто-закупку");
            toggleButton.setCallbackData("autobuy_stop");
        } else {
            toggleButton.setText("▶️ Запустить авто-закупку");
            toggleButton.setCallbackData("autobuy_start");
        }
        row1.add(toggleButton);
        keyboard.add(row1);
        
        // Кнопка статистики
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton statsButton = new InlineKeyboardButton();
        statsButton.setText("📊 Статистика");
        statsButton.setCallbackData("autobuy_stats");
        row2.add(statsButton);
        keyboard.add(row2);
        
        // Кнопка назад
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Назад");
        backButton.setCallbackData("back_to_main");
        row3.add(backButton);
        keyboard.add(row3);
        
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
        return "Управление автоматической закупкой новых токенов";
    }
}
