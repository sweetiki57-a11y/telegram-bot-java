#!/bin/bash

echo "🚀 Автоматический деплой на Heroku"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Проверка наличия Heroku CLI
if ! command -v heroku &> /dev/null; then
    echo -e "${RED}❌ Heroku CLI не установлен!${NC}"
    echo "Установите: brew install heroku/brew/heroku"
    exit 1
fi

# Проверка авторизации в Heroku
echo "🔐 Проверка авторизации в Heroku..."
if ! heroku auth:whoami &> /dev/null; then
    echo -e "${YELLOW}⚠️  Не авторизованы в Heroku${NC}"
    echo "Запускаю heroku login..."
    heroku login
fi

# Извлечение токенов из application.properties
CONFIG_FILE="src/main/resources/application.properties"

if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}❌ Файл конфигурации не найден: $CONFIG_FILE${NC}"
    exit 1
fi

echo "📋 Извлечение токенов из конфигурации..."
BOT_USERNAME=$(grep "telegram.bot.username" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')
BOT_TOKEN=$(grep "telegram.bot.token" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d ' ')

if [ -z "$BOT_USERNAME" ] || [ "$BOT_USERNAME" == "YOUR_BOT_USERNAME" ]; then
    echo -e "${RED}❌ Токен бота не настроен в $CONFIG_FILE${NC}"
    exit 1
fi

if [ -z "$BOT_TOKEN" ] || [ "$BOT_TOKEN" == "YOUR_BOT_TOKEN" ]; then
    echo -e "${RED}❌ Токен бота не настроен в $CONFIG_FILE${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Токены найдены:${NC}"
echo "   Username: $BOT_USERNAME"
echo "   Token: ${BOT_TOKEN:0:20}..."
echo ""

# Получение имени приложения Heroku
echo "🔍 Поиск приложения Heroku..."
HEROKU_APP=$(heroku apps 2>&1 | grep "telegram-bot" | tail -1 | awk '{print $1}')

if [ -z "$HEROKU_APP" ]; then
    echo -e "${YELLOW}⚠️  Приложение Heroku не найдено${NC}"
    echo "Создаю новое приложение..."
    HEROKU_APP="telegram-bot-java-$(date +%s)"
    heroku create "$HEROKU_APP" 2>&1 | grep -E "(Creating|https://)"
    
    # Добавляем remote если его нет
    if ! git remote | grep -q heroku; then
        git remote add heroku "https://git.heroku.com/$HEROKU_APP.git"
        echo -e "${GREEN}✅ Heroku remote добавлен${NC}"
    fi
else
    echo -e "${GREEN}✅ Найдено приложение: $HEROKU_APP${NC}"
fi

# Настройка переменных окружения
echo ""
echo "⚙️  Настройка переменных окружения..."
heroku config:set TELEGRAM_BOT_USERNAME="$BOT_USERNAME" --app "$HEROKU_APP"
heroku config:set TELEGRAM_BOT_TOKEN="$BOT_TOKEN" --app "$HEROKU_APP"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Переменные окружения настроены${NC}"
else
    echo -e "${RED}❌ Ошибка настройки переменных окружения${NC}"
    exit 1
fi

# Деплой
echo ""
echo "📦 Деплой на Heroku..."
echo "Это может занять 2-5 минут..."
git push heroku main

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Деплой успешен!${NC}"
else
    echo -e "${RED}❌ Ошибка деплоя${NC}"
    exit 1
fi

# Перезапуск приложения
echo ""
echo "🔄 Перезапуск приложения..."
heroku restart --app "$HEROKU_APP"

# Проверка статуса
echo ""
echo "📊 Проверка статуса..."
sleep 3
heroku ps --app "$HEROKU_APP"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ ВСЁ ГОТОВО!${NC}"
echo ""
echo "📱 Приложение: $HEROKU_APP"
echo "🔗 URL: https://dashboard.heroku.com/apps/$HEROKU_APP"
echo ""
echo "📋 Просмотр логов:"
echo "   heroku logs --tail --app $HEROKU_APP"
echo ""
echo "🧪 Проверка работы:"
echo "   1. Откройте Telegram"
echo "   2. Найдите бота: @$BOT_USERNAME"
echo "   3. Отправьте /start"
echo "   4. Увидите новый UI!"
echo ""
echo -e "${GREEN}🎉 ДЕПЛОЙ ЗАВЕРШЕН!${NC}"
