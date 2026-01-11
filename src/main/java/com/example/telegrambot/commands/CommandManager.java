package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;

import java.util.HashMap;
import java.util.Map;

/**
 * Command manager
 * Implements Command pattern for centralized command management
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
     * Initializes all commands
     */
    private void initializeCommands() {
        // Main commands
        commands.put("/start", new SendWelcomeCommand(bot));
        commands.put("/menu", new SendMainMenuCommand(bot));
        commands.put("/help", new SendHelpCommand(bot));
        commands.put("/admin", new AdminCommand(bot));
        
        // Button commands
        commands.put("🛒 Shops", new SendShopsCommand(bot));
        commands.put("💰 Exchangers", new SendExchangersCommand(bot));
        commands.put("🔍 Category Search", new SendSearchPromptCommand(bot));
        commands.put("📄 Header", new SendHeaderCommand(bot));
        commands.put("🏆 Top", new SendTopCommand(bot));
        commands.put("📋 Menu", new SendMainMenuCommand(bot));
        commands.put("🤖 Авто-торговля", new SendTradingCommand(bot));
        commands.put("💰 Кошелек", new SendWalletCommand(bot));
        commands.put("👤 Личный кабинет", new SendPersonalCabinetCommand(bot));
        commands.put("🛒 Авто-закупка", new SendAutoBuyCommand(bot));
    }
    
    /**
     * Executes command by key
     * @param commandKey command key
     * @param chatId chat ID
     * @return true if command found and executed
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
     * Checks if command exists
     * @param commandKey command key
     * @return true if command exists
     */
    public boolean hasCommand(String commandKey) {
        return commands.containsKey(commandKey);
    }
    
    /**
     * Gets command description
     * @param commandKey command key
     * @return command description or null if not found
     */
    public String getCommandDescription(String commandKey) {
        Command command = commands.get(commandKey);
        return command != null ? command.getDescription() : null;
    }
    
    /**
     * Gets all available commands
     * @return Map with commands
     */
    public Map<String, Command> getAllCommands() {
        return new HashMap<>(commands);
    }
}

