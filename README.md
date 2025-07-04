# EconomyEngine - Advanced Economy Plugin for Minecraft

[![Minecraft](https://img.shields.io/badge/Minecraft-1.16--1.19-green.svg)](https://www.minecraft.net)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/Version-1.5-orange.svg)](https://github.com/wryuin/EconomyEngine/releases)
# ! support end for a long time !
A feature-rich economy plugin for Paper/Spigot servers with multi-currency support, database flexibility, and PlaceholderAPI integration.


## Features ✨

- **Multi-Currency System**
  - Create unlimited currencies with `/economy create`
  - Custom symbols and automatic balancing
- **Database Support**
  - YAML (default)
  - SQLite (embedded)
  - MySQL (for large servers)
- **Balance Management**
  - Set/add/remove balances with commands
  - Player-to-player transactions
  - Top players leaderboard
- **PlaceholderAPI Integration**
  - Real-time balance display in chat/UI
  - Multiple formatting options
- **Auto-Save & Logging**
  - Configurable save intervals
  - Complete transaction history
- **User-Friendly GUI**
  - Manage currencies and balances visually
  - View top players and statistics

## Installation 📦

1. **Download** the latest JAR from [Releases]
2. **Place** in your `plugins/` folder
3. **Restart** your server
4. (Optional) Install [PlaceholderAPI] for placeholder support

## Commands & Permissions ⚙️

### Commands
```bash
/economy create <name> [symbol] # Create new currency
/economy set <amount> <currency> <player> # Set balance
/economy add <amount> <currency> <player> # Add funds
/economy remove <amount> <currency> <player> # Remove funds
/economy give <amount> <currency> <player> # Transfer funds
/economy top <currency> # View top 10 richest players
/economygui # Open the economy management GUI
```

### Permissions
```yaml
economyengine.use: # Base command access
economyengine.create: # Create currencies
economyengine.set: # Set balances
economyengine.add: # Add funds
economyengine.remove: # Remove funds
economyengine.give: # Transfer funds
economyengine.top: # View top players
economyengine.gui: # Access to economy GUI
```

### Placeholders 📊
```yaml
%EconomyEngine_value_<currency>%        # Raw balance (1000.5)
%EconomyEngine_value_<currency>_fixed%  # Formatted (1,000.50)
%EconomyEngine_value_<currency>_letter% # Shortened (1K, 1.5M)
%EconomyEngine_Top_<currency>_<place>%  # Player name at position (1-10)
%EconomyEngine_Top_<currency>_<place>_money% # Player balance at position (1-10)
```

## Configuration 🛠️
```yaml
# Database type: YAML, SQLITE, MYSQL
database:
  type: YAML

# Auto-save interval (minutes)
autosave:
  interval: 5

# MySQL Configuration (if used)
mysql:
  host: localhost
  port: 3306
  database: economy
  username: user
  password: password
```

## Database Setup 🗃️

### YAML (Default)
No setup required - uses data.yml

### SQLite
Automatically creates economy.db

### MySQL
1. Create database and user
2. Update MySQL config section
3. Set database type to "MYSQL"

## FAQ ❓

### Q: Can I use multiple currencies simultaneously?
A: Yes! Create as many currencies as needed.

### Q: How to migrate from YAML to MySQL?
A: Stop server → change config → restart. Data auto-migrates.

### Q: Does it work with Vault?
A: Not natively, but PlaceholderAPI can bridge most features.

### Q: How to backup data?
A: For YAML: Backup data.yml. For SQL: Standard DB backup procedures.

### Q: Is the plugin thread-safe?
A: Yes! Version 1.3.0 introduces enhanced thread safety and concurrent access handling.

## License 📄
MIT License

## Need Help?
Open an issue
