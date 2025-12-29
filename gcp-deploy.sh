#!/bin/bash

# GCP Cloud Run Deployment Script
# Make sure you have gcloud CLI installed and authenticated

echo "🚀 Deploying Telegram Bot to GCP Cloud Run..."

# Set your project ID (replace with your GCP project ID)
PROJECT_ID="your-project-id"
SERVICE_NAME="telegram-bot"
REGION="us-central1"

# Build the JAR
echo "📦 Building JAR file..."
mvn clean package -DskipTests

# Build and push Docker image to Google Container Registry
echo "🐳 Building Docker image..."
gcloud builds submit --tag gcr.io/${PROJECT_ID}/${SERVICE_NAME}

# Deploy to Cloud Run
echo "☁️ Deploying to Cloud Run..."
gcloud run deploy ${SERVICE_NAME} \
  --image gcr.io/${PROJECT_ID}/${SERVICE_NAME} \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --timeout 300 \
  --max-instances 1 \
  --set-env-vars TELEGRAM_BOT_USERNAME=${TELEGRAM_BOT_USERNAME},TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN}

echo "✅ Deployment complete!"
echo "Your bot is now running on Cloud Run!"

