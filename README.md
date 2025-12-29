# Telegram E-Commerce Bot

A professional, enterprise-grade Telegram bot for online store management with advanced payment processing, comprehensive test coverage, and clean architecture patterns.

## 🚀 Features

- 🛍️ **Product Catalog** - Organized product categories with advanced search functionality
- 🛒 **Shopping Cart** - Full cart management with add, remove, and quantity control
- 📋 **Order Management** - Complete order processing system with status tracking
- 💳 **Multi-Payment Support** - Cryptocurrency and Telegram Stars payment methods
- 🔗 **Payment Integration** - Seamless payment flow with automatic refund handling
- 👤 **Admin Panel** - Comprehensive admin interface for product and order management
- 🔍 **Category Search** - Advanced search by product categories with keyword matching
- 📊 **Statistics** - Sales analytics and product statistics dashboard
- ✅ **Comprehensive Testing** - 78+ unit and integration tests with 100% payment flow coverage

## 🛠️ Technology Stack

- **Java 11** - Core programming language with modern features
- **Maven** - Build automation, dependency management, and test execution
- **Telegram Bot API 6.8.0** - Official Telegram Bot framework
- **JUnit 5** - Modern testing framework for unit and integration tests
- **Mockito** - Mocking framework for isolated unit testing
- **Jackson** - High-performance JSON processing library
- **SLF4J** - Logging framework with simple implementation

## 🏗️ Architecture & Design Patterns

The bot follows enterprise-grade software architecture principles with clean code practices:

### Design Patterns Implemented

1. **Strategy Pattern** - Payment method abstraction
   - `PaymentMethod` interface for payment processing strategies
   - `CryptoPaymentMethod` - Blockchain transaction processing
   - `StarsPaymentMethod` - Telegram Stars payment processing
   - Easy to extend with new payment methods

2. **Factory Pattern** - Payment method creation
   - `PaymentMethodFactory` - Centralized payment method instantiation
   - Type-safe payment method creation
   - Supports enum and string-based creation

3. **Command Pattern** - Command handling
   - `CommandManager` - Centralized command dispatcher
   - `BaseCommand` - Abstract base for all commands
   - Clean separation of command logic

4. **Factory Pattern (UI)** - Keyboard creation
   - `KeyboardFactory` - Centralized UI element creation
   - Consistent keyboard layouts
   - Reusable UI components

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│              Telegram Bot API Layer                     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│           MyTelegramBot (Main Handler)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Command      │  │ Payment     │  │ Order       │  │
│  │ Manager      │  │ Processor   │  │ Manager     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──────┐ ┌──▼────┐ ┌─────▼──────┐
│  Payment     │ │ Cart  │ │  Product   │
│  Strategies  │ │ Mgmt  │ │  Catalog   │
└──────────────┘ └───────┘ └─────────────┘
```

## 💳 Payment System

### Supported Payment Methods

1. **Cryptocurrency Payment** (₿)
   - Blockchain transaction verification
   - Automatic error handling and refund processing
   - 5% simulated failure rate for testing

2. **Telegram Stars Payment** (⭐)
   - Native Telegram payment integration
   - Optimized for Telegram ecosystem
   - 3% simulated failure rate for testing

### Payment Flow

```
User Checkout → Payment Method Selection → Payment Processing
                                              ├─ Success → Show Payment Link
                                              └─ Failure → Refund Notification
```

### Payment Features

- **Automatic Refund** - Failed payments trigger automatic refund process
- **Error Handling** - Comprehensive error handling with user-friendly messages
- **Payment Link** - Secure payment group link provided only on successful processing
- **Order Cancellation** - Automatic order cancellation on payment failure
- **Cart Preservation** - Cart preserved on payment failure for retry

## 📋 Project Structure

```
src/main/java/com/example/telegrambot/
├── TelegramBotApplication.java    # Application entry point
├── MyTelegramBot.java             # Main bot logic and handlers
├── commands/                       # Command handlers (Command pattern)
│   ├── CommandManager.java        # Command dispatcher
│   ├── BaseCommand.java           # Base command class
│   └── [Command implementations]
├── payment/                       # Payment system (Strategy pattern)
│   ├── PaymentMethod.java         # Payment strategy interface
│   ├── CryptoPaymentMethod.java   # Cryptocurrency implementation
│   ├── StarsPaymentMethod.java    # Telegram Stars implementation
│   ├── PaymentProcessor.java     # Payment processing orchestrator
│   ├── PaymentResult.java         # Payment result model
│   └── PaymentMethodFactory.java  # Payment method factory
├── factory/                       # UI factories (Factory pattern)
│   └── KeyboardFactory.java      # Keyboard creation factory
├── Product.java                   # Product model
├── Cart.java                      # Shopping cart model
├── Order.java                     # Order model
├── OrderManager.java              # Order management
└── AdminPanel.java                # Admin interface

src/test/java/com/example/telegrambot/
└── payment/                       # Payment system tests
    ├── PaymentMethodTest.java     # Payment method unit tests
    ├── PaymentProcessorTest.java  # Processor unit tests
    ├── PaymentMethodFactoryTest.java # Factory unit tests
    └── PaymentIntegrationTest.java   # Integration tests
```

## 🧪 Testing

### Test Coverage

- **78 comprehensive tests** covering all payment flows
- **Positive test cases** - Successful payment scenarios
- **Negative test cases** - Error handling and edge cases
- **Integration tests** - End-to-end payment flow validation
- **Unit tests** - Isolated component testing

### Test Categories

1. **PaymentMethodTest** (48 tests)
   - Payment method creation and validation
   - Success and failure scenarios
   - Edge cases (null, empty, invalid data)

2. **PaymentProcessorTest** (6 tests)
   - Payment processing logic
   - Method switching
   - Error handling

3. **PaymentMethodFactoryTest** (8 tests)
   - Factory pattern validation
   - Type creation
   - Error handling

4. **PaymentIntegrationTest** (16 tests)
   - Complete payment flows
   - Link generation validation
   - Multiple payment sequences

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentMethodTest

# Run with coverage report
mvn test jacoco:report
```

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

**With Tests:**
```bash
mvn clean test compile exec:java
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

### Payment System
- **Strategy Pattern** for payment method abstraction
- **Factory Pattern** for payment method creation
- **Automatic error handling** and refund processing
- **Secure payment link** generation

### Command System
- **Command Pattern** for clean command handling
- Centralized command management
- Easy to extend with new commands

### Order Management
- Complete order lifecycle management
- Status tracking (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
- User order history

### Admin Panel
Full-featured admin interface for:
- Product management
- Order tracking and management
- Sales statistics
- Category management
- Product refresh and generation

## 📈 Code Quality

- **Clean Code Principles** - SOLID design patterns throughout
- **Separation of Concerns** - Clear layer separation
- **Test-Driven Development** - Comprehensive test coverage
- **Error Handling** - Robust error handling and validation
- **Code Documentation** - Well-documented codebase
- **Maintainability** - Easy to extend and modify

## 🚀 Future Enhancements

- Database integration (PostgreSQL/MySQL)
- Real-time payment webhook integration
- Advanced analytics dashboard
- Multi-language support
- Payment method expansion (credit cards, PayPal, etc.)
- Inventory management system
- Customer support integration

## 📝 License

MIT License

## 👨‍💻 Development

Built with enterprise-grade software engineering practices:
- **Design Patterns** - Strategy, Factory, Command patterns
- **Clean Architecture** - Layered architecture with clear boundaries
- **Test Coverage** - Comprehensive unit and integration tests
- **Code Quality** - SOLID principles and clean code practices
- **Scalability** - Designed for easy extension and scaling

---

**Professional Development** - This bot demonstrates production-ready code with proper architecture, testing, and maintainability practices suitable for enterprise environments.
