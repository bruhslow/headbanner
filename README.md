# HeadBanner

A lightweight Paper plugin that allows players to wear banners on their head as cosmetic hats, similar to raid captains.

---

### Direct Download

* **Latest Release**: [Download `HeadBanner.jar`](https://github.com/bruhslow/headbanner/releases/latest)
* **Repository Build**: [`build/libs/HeadBanner.jar`](build/libs/HeadBanner.jar)

---

### Overview

| Detail | Specification |
| :--- | :--- |
| **Platform** | Paper / Purpur / Spigot |
| **Minecraft Versions** | 1.20+ / 26.x |
| **Java Version** | 21+ (Java 25 recommended for 26.x) |
| **License** | MIT |

---

### Key Features

* **Equip Any Banner**: Equip any standard, patterned, or custom ominous banner onto the player's helmet slot.
* **Intuitive Interaction**: Right-click while holding a banner to equip or swap hats, or execute the `/headbanner` command.
* **Safe Inventory Handling**: Preserves existing helmet items and returns them safely to inventory without dropping or item loss.
* **Lore & Pattern Preservation**: Full support for complex banner patterns, colors, custom names, and lore tags.
* **Lightweight & Lag-Free**: Zero background ticking tasks or server overhead.

---

### Commands & Permissions

| Command | Arguments | Permission | Description |
| :--- | :--- | :--- | :--- |
| `/headbanner` | None | `headbanner.use` | Equips the banner held in hand onto the player's head |
| `/headbanner reload` | None | `headbanner.admin` | Reloads plugin configuration and settings |

**Aliases**: `/bannerhat`, `/hbanner`

---

### Quick Start

1. Download **`HeadBanner.jar`** and place it in your server's `plugins/` directory.
2. Restart or reload your server.
3. Hold any banner in your main hand and right-click or run `/headbanner`.
4. Your banner is now equipped as a captain hat.

---

### Building from Source

Prerequisites: JDK 21+.

```bash
# Clone the repository
git clone https://github.com/bruhslow/headbanner.git

# Build the plugin archive
./gradlew build
```

Compiled JARs will be generated in `build/libs/`.

---

### License

This project is licensed under the MIT License.
