#!/bin/bash

# Скрипт для запуска Telegram бота

echo "🤖 Запуск Telegram бота для онлайн продаж..."

# Настраиваем PATH для Java 11
export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"

# Проверяем наличие конфигурационного файла
if [ ! -f "src/main/resources/application.properties" ]; then
    echo "❌ Файл конфигурации не найден!"
    echo "📝 Скопируйте application.properties.example в src/main/resources/application.properties"
    echo "   и настройте токен бота"
    exit 1
fi

# Проверяем наличие Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven не найден! Установите Maven для запуска проекта."
    echo "💡 Выполните: brew install maven"
    exit 1
fi

# Проверяем Java
if ! command -v java &> /dev/null; then
    echo "❌ Java не найдена! Установите Java 11+ для запуска проекта."
    echo "💡 Выполните: brew install openjdk@11"
    exit 1
fi

echo "✅ Java и Maven найдены!"

# Собираем проект
echo "🔨 Сборка проекта..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "❌ Ошибка при сборке проекта!"
    exit 1
fi

# Запускаем бота
echo "🚀 Запуск бота..."
echo "📱 Бот будет доступен по адресу: @fredo_store_bot"
echo "⏹️  Для остановки нажмите Ctrl+C"
echo ""
mvn exec:java
