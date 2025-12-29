package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер команд
 * Реализует паттерн Command для централизованного управления командами
 */
public class CommandManager {
    private final Map<String, Command> commands;
    private final MyTelegramBot bot;
    
    public CommandManager(MyTelegramBot bot) {
        this.bot = bot;
        this.commands = new HashMap<>();
        initializeCommands();
    }
    
    /**
     * Инициализирует все команды
     */
    private void initializeCommands() {
        // Основные команды
        commands.put("/start", new SendWelcomeCommand(bot));
        commands.put("/menu", new SendMainMenuCommand(bot));
        commands.put("/help", new SendHelpCommand(bot));
        commands.put("/admin", new AdminCommand(bot));
        
        // Команды кнопок
        commands.put("🛒 Шопы", new SendShopsCommand(bot));
        commands.put("💰 Обменники", new SendExchangersCommand(bot));
        commands.put("🔍 Поиск по категориям", new SendSearchPromptCommand(bot));
        commands.put("📄 Шапка", new SendHeaderCommand(bot));
        commands.put("🏆 Топ", new SendTopCommand(bot));
        commands.put("📋 Меню", new SendMainMenuCommand(bot));
    }
    
    /**
     * Выполняет команду по ключу
     * @param commandKey ключ команды
     * @param chatId ID чата
     * @return true если команда найдена и выполнена
     */
    public boolean executeCommand(String commandKey, long chatId) {
        Command command = commands.get(commandKey);
        if (command != null) {
            command.execute(chatId);
            return true;
        }
        return false;
    }
    
    /**
     * Проверяет, существует ли команда
     * @param commandKey ключ команды
     * @return true если команда существует
     */
    public boolean hasCommand(String commandKey) {
        return commands.containsKey(commandKey);
    }
    
    /**
     * Получает описание команды
     * @param commandKey ключ команды
     * @return описание команды или null если команда не найдена
     */
    public String getCommandDescription(String commandKey) {
        Command command = commands.get(commandKey);
        return command != null ? command.getDescription() : null;
    }
    
    /**
     * Получает все доступные команды
     * @return Map с командами
     */
    public Map<String, Command> getAllCommands() {
        return new HashMap<>(commands);
    }
}

