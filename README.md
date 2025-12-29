# Telegram E-Commerce Bot

A professional Telegram bot for online store management with product catalog, shopping cart, and order processing capabilities.

## 🚀 Features

- 🛍️ **Product Catalog** - Organized product categories with search functionality
- 🛒 **Shopping Cart** - Add, remove, and manage items
- 📋 **Order Management** - Complete order processing system
- 👤 **Admin Panel** - Product and order management interface
- 🔍 **Category Search** - Advanced search by product categories
- 📊 **Statistics** - Sales and product analytics

## 🛠️ Technologies

- **Java 11** - Core programming language
- **Maven** - Build automation and dependency management
- **Telegram Bot API 6.8.0** - Official Telegram Bot framework
- **Jackson** - JSON processing
- **SLF4J** - Logging framework

## 📋 Architecture

The bot follows a clean architecture pattern with separation of concerns:

- **Command Pattern** - Centralized command management
- **Factory Pattern** - Keyboard and UI element creation
- **Manager Classes** - Order and product management
- **Modular Design** - Easy to extend and maintain

## ⚙️ Setup

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Telegram Bot Token (from @BotFather)

### Configuration

1. Copy `application.properties.example` to `src/main/resources/application.properties`
2. Configure your bot credentials:

```properties
telegram.bot.username=YOUR_BOT_USERNAME
telegram.bot.token=YOUR_BOT_TOKEN
```

3. Add admin IDs in `AdminPanel.java`:

```java
ADMIN_IDS.add(YOUR_TELEGRAM_ID);
```

### Running the Bot

**Quick Start:**
```bash
chmod +x run.sh
./run.sh
```

**Manual:**
```bash
mvn clean compile
mvn exec:java
```

## 📁 Project Structure

```
src/main/java/com/example/telegrambot/
├── TelegramBotApplication.java  # Application entry point
├── MyTelegramBot.java          # Main bot logic
├── commands/                   # Command handlers
│   ├── CommandManager.java     # Command dispatcher
│   └── BaseCommand.java        # Base command class
├── factory/
│   └── KeyboardFactory.java    # UI keyboard factory
├── Product.java                # Product model
├── Cart.java                   # Shopping cart model
├── Order.java                  # Order model
├── OrderManager.java           # Order management
└── AdminPanel.java             # Admin interface
```

## 🎯 Bot Commands

### User Commands
- `/start` - Welcome message and main menu
- `/menu` - Browse product catalog
- `/cart` - View shopping cart
- `/orders` - View order history
- `/help` - Show help information

### Admin Commands
- `/admin` - Access admin panel (admin only)

## 🔧 Key Components

### Command System
Implements Command pattern for clean command handling and easy extensibility.

### Keyboard Factory
Centralized keyboard creation using Factory pattern for consistent UI.

### Order Management
Complete order lifecycle management with status tracking.

### Admin Panel
Full-featured admin interface for:
- Product management
- Order tracking
- Sales statistics
- Category management

## 📈 Future Enhancements

- Database integration (PostgreSQL/MySQL)
- Payment gateway integration
- Real-time notifications
- Advanced analytics dashboard
- Multi-language support

## 📝 License

MIT License

## 👨‍💻 Development

Built with clean code principles, following SOLID design patterns for maintainability and scalability.
