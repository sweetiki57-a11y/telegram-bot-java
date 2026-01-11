#!/bin/bash

# Скрипт для проверки готовности к деплою

echo "🔍 Проверка готовности проекта к деплою..."
echo ""

ERRORS=0

# 1. Проверка Java файлов
echo "1️⃣ Проверка Java файлов..."
JAVA_COUNT=$(find src/main/java -name "*.java" | wc -l | tr -d ' ')
if [ "$JAVA_COUNT" -gt 0 ]; then
    echo "   ✅ Найдено $JAVA_COUNT Java файлов"
else
    echo "   ❌ Java файлы не найдены!"
    ERRORS=$((ERRORS + 1))
fi

# 2. Проверка компиляции
echo "2️⃣ Проверка компиляции..."
mvn clean compile -q > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Проект компилируется успешно"
else
    echo "   ❌ Ошибки компиляции!"
    ERRORS=$((ERRORS + 1))
fi

# 3. Проверка конфигурации
echo "3️⃣ Проверка конфигурации..."
if [ -f "src/main/resources/application.properties" ]; then
    echo "   ✅ application.properties найден"
    
    # Проверяем наличие обязательных параметров
    if grep -q "telegram.bot.token" src/main/resources/application.properties && \
       ! grep -q "YOUR_BOT_TOKEN" src/main/resources/application.properties; then
        echo "   ✅ Токен бота настроен"
    else
        echo "   ⚠️  Токен бота не настроен (используется YOUR_BOT_TOKEN)"
    fi
    
    if grep -q "backend.api.url" src/main/resources/application.properties; then
        echo "   ✅ Back-end API URL настроен"
    else
        echo "   ⚠️  Back-end API URL не настроен"
    fi
else
    echo "   ⚠️  application.properties не найден (будет создан из примера)"
fi

# 4. Проверка зависимостей
echo "4️⃣ Проверка зависимостей..."
if [ -f "pom.xml" ]; then
    echo "   ✅ pom.xml найден"
    mvn dependency:resolve -q > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "   ✅ Все зависимости разрешены"
    else
        echo "   ⚠️  Проблемы с зависимостями"
    fi
else
    echo "   ❌ pom.xml не найден!"
    ERRORS=$((ERRORS + 1))
fi

# 5. Проверка ключевых компонентов
echo "5️⃣ Проверка ключевых компонентов..."
COMPONENTS=(
    "src/main/java/com/example/telegrambot/TelegramBotApplication.java"
    "src/main/java/com/example/telegrambot/MyTelegramBot.java"
    "src/main/java/com/example/telegrambot/trading/AutoTradingEngine.java"
    "src/main/java/com/example/telegrambot/trading/WalletService.java"
    "src/main/java/com/example/telegrambot/commands/SendTradingCommand.java"
    "src/main/java/com/example/telegrambot/commands/SendWalletCommand.java"
)

for component in "${COMPONENTS[@]}"; do
    if [ -f "$component" ]; then
        echo "   ✅ $(basename $component)"
    else
        echo "   ❌ Отсутствует: $(basename $component)"
        ERRORS=$((ERRORS + 1))
    fi
done

# 6. Проверка сборки JAR
echo "6️⃣ Проверка сборки JAR..."
mvn package -DskipTests -q > /dev/null 2>&1
if [ -f "target/telegram-bot-1.0.0.jar" ]; then
    echo "   ✅ JAR файл собран успешно"
    JAR_SIZE=$(du -h target/telegram-bot-1.0.0.jar | cut -f1)
    echo "   📦 Размер: $JAR_SIZE"
else
    echo "   ❌ JAR файл не собран!"
    ERRORS=$((ERRORS + 1))
fi

# Итоги
echo ""
if [ $ERRORS -eq 0 ]; then
    echo "✅ Все проверки пройдены! Проект готов к деплою."
    exit 0
else
    echo "❌ Найдено $ERRORS ошибок. Исправьте их перед деплоем."
    exit 1
fi
