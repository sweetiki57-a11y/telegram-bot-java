#!/bin/bash

# Скрипт для перезапуска Telegram бота

echo "🔄 Перезапуск Telegram бота..."

# Останавливаем старый процесс если запущен
echo "⏹️  Остановка старого процесса..."
pkill -f "TelegramBotApplication" || true
sleep 2

# Проверяем наличие конфигурации
if [ ! -f "src/main/resources/application.properties" ]; then
    echo "⚠️  Файл конфигурации не найден!"
    echo "📝 Создаю из примера..."
    mkdir -p src/main/resources
    cp application.properties.example src/main/resources/application.properties
    echo "✅ Файл создан. Пожалуйста, настройте токен бота в src/main/resources/application.properties"
    echo "   telegram.bot.username=YOUR_BOT_USERNAME"
    echo "   telegram.bot.token=YOUR_BOT_TOKEN"
    echo "   backend.api.url=http://your-backend:8080/api"
    exit 1
fi

# Очищаем и собираем проект
echo "🔨 Очистка и сборка проекта..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Ошибка при сборке проекта!"
    exit 1
fi

# Запускаем бота
echo "🚀 Запуск бота..."
echo "📱 Бот будет доступен в Telegram"
echo "⏹️  Для остановки нажмите Ctrl+C или запустите: pkill -f TelegramBotApplication"
echo ""

# Запускаем в фоне с перенаправлением логов
nohup java -jar target/telegram-bot-1.0.0.jar > bot.log 2>&1 &

# Ждем немного и проверяем что процесс запустился
sleep 3
if pgrep -f "TelegramBotApplication" > /dev/null; then
    echo "✅ Бот успешно запущен!"
    echo "📋 Логи: tail -f bot.log"
    echo "🆔 PID: $(pgrep -f TelegramBotApplication)"
else
    echo "❌ Бот не запустился. Проверьте логи: cat bot.log"
    exit 1
fi
