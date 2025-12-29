# Deployment Guide

## Free Hosting Options for Java Telegram Bot

### 1. **Railway** (Recommended) ⭐
- **Free Tier**: $5 credit/month
- **Pros**: 
  - Very easy deployment
  - Automatic builds from GitHub
  - Supports Java/Maven
  - Free SSL
  - Good for Telegram bots
- **Deployment**: Connect GitHub repo, Railway auto-detects Maven
- **URL**: https://railway.app

### 2. **Render**
- **Free Tier**: Available
- **Pros**:
  - Free tier with limitations
  - Auto-deploy from GitHub
  - Supports Java
  - Free SSL
- **Cons**: 
  - Spins down after inactivity (15 min)
  - Slower cold starts
- **URL**: https://render.com

### 3. **Fly.io**
- **Free Tier**: 3 shared-cpu VMs
- **Pros**:
  - Good performance
  - Global deployment
  - Supports Java
- **Cons**: 
  - More complex setup
  - Need Dockerfile
- **URL**: https://fly.io

### 4. **Oracle Cloud Always Free**
- **Free Tier**: 2 VMs (ARM64)
- **Pros**:
  - Full VPS control
  - Always free (no credit card needed)
  - Good for long-running bots
- **Cons**: 
  - Need to set up everything manually
  - More technical
- **URL**: https://www.oracle.com/cloud/free/

### 5. **Heroku** (Alternative)
- **Free Tier**: Discontinued (now paid only)
- **Note**: No longer offers free tier

## Recommended: Railway Setup

### Step 1: Create Railway Account
1. Go to https://railway.app
2. Sign up with GitHub
3. Get $5 free credit monthly

### Step 2: Deploy from GitHub
1. Click "New Project"
2. Select "Deploy from GitHub repo"
3. Choose your `telegram-bot-java` repository
4. Railway will auto-detect Maven project

### Step 3: Configure Environment Variables
Add these in Railway dashboard:
```
TELEGRAM_BOT_USERNAME=your_bot_username
TELEGRAM_BOT_TOKEN=your_bot_token
```

### Step 4: Set Build Command
Railway should auto-detect, but if needed:
```bash
mvn clean package
```

### Step 5: Set Start Command
```bash
java -jar target/telegram-bot-1.0.0.jar
```

Or use Maven:
```bash
mvn exec:java -Dexec.mainClass="com.example.telegrambot.TelegramBotApplication"
```

## Alternative: Render Setup

### Step 1: Create Render Account
1. Go to https://render.com
2. Sign up with GitHub

### Step 2: Create New Web Service
1. Connect GitHub repository
2. Select "Web Service"
3. Choose your repo

### Step 3: Configure
- **Build Command**: `mvn clean package`
- **Start Command**: `java -jar target/telegram-bot-1.0.0.jar`
- **Environment**: Java

### Step 4: Add Environment Variables
- `TELEGRAM_BOT_USERNAME`
- `TELEGRAM_BOT_TOKEN`

## For Paid Subscriptions

If you have paid subscriptions, check:

### GitHub
- **GitHub Actions**: Can run workflows, but not for 24/7 hosting
- **GitHub Codespaces**: For development, not production hosting

### JetBrains
- **JetBrains Space**: Has hosting capabilities
- Check if your subscription includes hosting

### AWS Free Tier
- **EC2**: 750 hours/month free (t2.micro)
- **Elastic Beanstalk**: Free tier available
- Good for learning, but need credit card

### Google Cloud
- **App Engine**: Free tier available
- **Cloud Run**: Free tier with limits
- Need credit card for verification

## Quick Start: Railway (Easiest) ⭐ RECOMMENDED

### Why Railway?
- **$5 free credit/month** (enough for a bot)
- **Super easy** - just connect GitHub
- **Auto-deploy** on every push
- **24/7 uptime** on free tier
- **No credit card needed** for free tier

### Step-by-Step:

1. **Sign up**: https://railway.app
   - Click "Start a New Project"
   - Sign in with GitHub

2. **Deploy from GitHub**:
   - Click "Deploy from GitHub repo"
   - Select your `telegram-bot-java` repository
   - Railway auto-detects Maven project

3. **Add Environment Variables**:
   - Go to "Variables" tab
   - Add:
     ```
     TELEGRAM_BOT_USERNAME=your_bot_username
     TELEGRAM_BOT_TOKEN=your_bot_token
     ```

4. **Deploy**:
   - Railway automatically:
     - Builds with Maven
     - Creates JAR file
     - Runs your bot 24/7
   - Check "Deployments" tab for logs

5. **Monitor**:
   - View logs in real-time
   - Check usage in dashboard
   - Bot runs automatically!

### Railway Configuration:
- **Build Command**: `mvn clean package -DskipTests` (auto-detected)
- **Start Command**: `java -jar target/telegram-bot-1.0.0.jar`
- **Config file**: `railway.json` (already created)

**That's it!** Your bot will be live in 2-3 minutes.

## Notes

- Telegram bots don't need public URL (they use webhooks or long polling)
- Railway/Render handle this automatically
- Bot will run 24/7 on free tier (with Railway's $5 credit)
- Monitor usage in dashboard

