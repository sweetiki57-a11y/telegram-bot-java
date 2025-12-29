# Heroku Deployment Guide

## Quick Deploy to Heroku

### Method 1: Heroku CLI (Recommended)

#### Step 1: Install Heroku CLI
```bash
# macOS
brew tap heroku/brew && brew install heroku

# Or download from: https://devcenter.heroku.com/articles/heroku-cli
```

#### Step 2: Login to Heroku
```bash
heroku login
```

#### Step 3: Create Heroku App
```bash
# Navigate to project directory
cd /Users/Apple/telegram-bot-java

# Create app (choose unique name)
heroku create your-bot-name

# Or let Heroku generate name
heroku create
```

#### Step 4: Set Environment Variables
```bash
heroku config:set TELEGRAM_BOT_USERNAME=your_bot_username
heroku config:set TELEGRAM_BOT_TOKEN=your_bot_token
```

#### Step 5: Deploy
```bash
# Deploy from Git
git push heroku main

# Or if using master branch
git push heroku master
```

#### Step 6: Check Logs
```bash
heroku logs --tail
```

### Method 2: GitHub Integration (Easier)

#### Step 1: Push to GitHub
```bash
git push origin main
```

#### Step 2: Connect GitHub to Heroku
1. Go to https://dashboard.heroku.com/apps
2. Click "New" → "Create new app"
3. Enter app name (e.g., `telegram-bot-java`)
4. Choose region (US or Europe)
5. Click "Create app"

#### Step 3: Connect GitHub Repository
1. In your app dashboard, go to "Deploy" tab
2. Under "Deployment method", select "GitHub"
3. Click "Connect to GitHub"
4. Authorize Heroku
5. Search for your repository: `telegram-bot-java`
6. Click "Connect"

#### Step 4: Configure Environment Variables
1. Go to "Settings" tab
2. Click "Reveal Config Vars"
3. Add:
   - `TELEGRAM_BOT_USERNAME` = `your_bot_username`
   - `TELEGRAM_BOT_TOKEN` = `your_bot_token`

#### Step 5: Enable Automatic Deploys (Optional)
1. In "Deploy" tab
2. Under "Automatic deploys"
3. Select branch: `main`
4. Click "Enable Automatic Deploys"

#### Step 6: Manual Deploy
1. In "Deploy" tab
2. Under "Manual deploy"
3. Select branch: `main`
4. Click "Deploy Branch"

#### Step 7: View Logs
1. Go to "More" → "View logs"
2. Or use CLI: `heroku logs --tail --app your-app-name`

## Configuration Files

### Procfile
```
web: java -jar target/telegram-bot-1.0.0.jar
```
- Tells Heroku how to run your app
- Already created in project

### system.properties
```
java.runtime.version=11
```
- Specifies Java version
- Already created in project

### pom.xml
- Maven configuration
- Already configured with shade plugin for JAR creation

## Build Configuration

Heroku will automatically:
1. Detect Maven project (pom.xml)
2. Run `mvn clean package`
3. Execute JAR from Procfile

## Monitoring

### View Logs
```bash
heroku logs --tail
```

### Check App Status
```bash
heroku ps
```

### Restart App
```bash
heroku restart
```

### Scale Dynos
```bash
# Free tier: 1 web dyno
heroku ps:scale web=1

# Paid: Scale up
heroku ps:scale web=2
```

## Troubleshooting

### Build Fails
- Check logs: `heroku logs --tail`
- Verify Java version in `system.properties`
- Ensure JAR builds locally: `mvn clean package`

### App Crashes
- Check logs for errors
- Verify environment variables are set
- Ensure bot token is correct

### Bot Not Responding
- Check if app is running: `heroku ps`
- View logs: `heroku logs --tail`
- Verify Telegram bot token

## Heroku Pricing

### Free Tier (Discontinued)
- No longer available

### Eco Dyno ($5/month)
- 1000 hours/month
- Sleeps after 30 min inactivity
- Good for development/testing

### Basic Dyno ($7/month)
- Always on
- No sleep
- Perfect for Telegram bots

### Recommended: Basic Dyno
- Telegram bots need to be always on
- $7/month is reasonable
- No sleep = instant responses

## Quick Commands Reference

```bash
# Create app
heroku create app-name

# Set config vars
heroku config:set KEY=value

# View config vars
heroku config

# Deploy
git push heroku main

# View logs
heroku logs --tail

# Restart
heroku restart

# Open app (if web interface needed)
heroku open

# Run one-off command
heroku run java -version
```

## Next Steps After Deploy

1. ✅ Verify bot is running: `heroku logs --tail`
2. ✅ Test bot in Telegram
3. ✅ Set up monitoring (optional)
4. ✅ Enable automatic deploys from GitHub

## Support

- Heroku Docs: https://devcenter.heroku.com/articles/getting-started-with-java
- Heroku Support: https://help.heroku.com

