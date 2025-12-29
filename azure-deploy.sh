#!/bin/bash

# Azure App Service Deployment Script
# Requires: Azure CLI installed and logged in

echo "🚀 Deploying Telegram Bot to Azure App Service..."

# Set variables
RESOURCE_GROUP="telegram-bot-rg"
APP_NAME="telegram-bot-$(date +%s)"
PLAN_NAME="telegram-bot-plan"
LOCATION="eastus"

# Create resource group (if not exists)
echo "📦 Creating resource group..."
az group create --name ${RESOURCE_GROUP} --location ${LOCATION}

# Create App Service plan (Free tier)
echo "📦 Creating App Service plan..."
az appservice plan create \
  --name ${PLAN_NAME} \
  --resource-group ${RESOURCE_GROUP} \
  --sku FREE \
  --is-linux

# Create web app
echo "📦 Creating web app..."
az webapp create \
  --resource-group ${RESOURCE_GROUP} \
  --plan ${PLAN_NAME} \
  --name ${APP_NAME} \
  --runtime "JAVA:11-java11"

# Build JAR
echo "📦 Building JAR file..."
mvn clean package -DskipTests

# Deploy JAR
echo "☁️ Deploying to Azure..."
az webapp deploy \
  --resource-group ${RESOURCE_GROUP} \
  --name ${APP_NAME} \
  --type jar \
  --src-path target/telegram-bot-1.0.0.jar

# Set environment variables
echo "🔧 Setting environment variables..."
az webapp config appsettings set \
  --resource-group ${RESOURCE_GROUP} \
  --name ${APP_NAME} \
  --settings TELEGRAM_BOT_USERNAME=${TELEGRAM_BOT_USERNAME} TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}

echo "✅ Deployment complete!"
echo "Your bot is now running on Azure App Service!"
echo "App URL: https://${APP_NAME}.azurewebsites.net"

