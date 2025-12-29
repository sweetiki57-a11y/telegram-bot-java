#!/bin/bash

# AWS Elastic Beanstalk Deployment Script
# Requires: AWS CLI, EB CLI installed

echo "🚀 Deploying Telegram Bot to AWS Elastic Beanstalk..."

# Initialize EB (first time only)
# eb init -p java-11 telegram-bot --region us-east-1

# Create environment (first time only)
# eb create telegram-bot-env

# Build the JAR
echo "📦 Building JAR file..."
mvn clean package -DskipTests

# Create deployment package
echo "📦 Creating deployment package..."
mkdir -p deploy
cp target/telegram-bot-1.0.0.jar deploy/
cp Procfile deploy/ 2>/dev/null || echo "web: java -jar telegram-bot-1.0.0.jar" > deploy/Procfile
cd deploy
zip -r ../telegram-bot.zip .
cd ..

# Deploy to Elastic Beanstalk
echo "☁️ Deploying to AWS..."
eb deploy

echo "✅ Deployment complete!"
echo "Your bot is now running on AWS Elastic Beanstalk!"

