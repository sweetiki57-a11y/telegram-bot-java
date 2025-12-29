package com.example.telegrambot.commands;

/**
 * Интерфейс для всех команд бота
 * Реализует паттерн Command для инкапсуляции запросов
 */
public interface Command {
    /**
     * Выполняет команду
     * @param chatId ID чата, в котором выполняется команда
     */
    void execute(long chatId);
    
    /**
     * Возвращает описание команды
     * @return описание команды
     */
    String getDescription();
}

