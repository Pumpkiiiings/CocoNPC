<div align="center">

<img src="https://i.ibb.co/gC6y94s/cocologo.png" alt="CocoNPC" width="560">

**[Documentation](https://coconpc.vercel.app)** &nbsp;•&nbsp; **[Support &amp; bug reports](https://discord.gg/ydsUw5UJrB)**

[![PaperMC](https://img.shields.io/badge/PaperMC-1.21+-343434?style=flat-square&logo=papermc)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=flat-square&logo=java)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

</div>

---

**CocoNPC** is a revolutionary, lightweight, and ultra-modular NPC management plugin for modern PaperMC servers (1.21+). Leaving traditional, heavy fake-player systems behind, CocoNPC leverages Minecraft's native `ItemDisplay` and `TextDisplay` entities to deliver extreme modularity, precise anatomical animations, and zero fake-player packet overhead.

---

## 🌟 Why CocoNPC?

* 🚀 **Zero Fake-Player Overhead:** Say goodbye to tab-list glitches and heavy NPC plugins. CocoNPC uses native Display Entities for maximum performance.
* 🧩 **11-Part Hierarchical Anatomy:** NPCs are built using interconnected parts (Head, Torso, Upper/Lower Arms, Upper/Lower Legs). Moving an arm dynamically moves the forearm!
* 🤸 **Custom Joint Bending:** Sit, wave, lay down, or cross arms. Bend knees and elbows independently for lifelike custom postures.
* 🎨 **Smart Skin Management:** Drop a `.png` into the `/skins/` folder and spawn it instantly. CocoNPC automatically crops, fixes slim (Alex) skins to classic (Steve) geometry, and caches them using the MineSkin API.
* ⚡ **Native TextDisplays:** High-performance, built-in holograms without relying on external dependencies like DecentHolograms.

---

## 🕹️ In-Game 3D Editor

Creating the perfect scene has never been easier. Use our in-game visual editor to pose your NPCs anywhere in 3D space.

1. **Shift + Right-Click** an NPC to open the GUI Editor (`NpcEditorMenu`).
2. **Left-Click** any body part to enter **3D Editing Mode**.
3. **Switch Modes** by **Right-Clicking the air** or pressing **'F' (Swap Hand)**.

| Editing Mode | `Scroll` (Normal) | `Shift + Scroll` (Precision) |
| :--- | :--- | :--- |
| 🔄 **1. Rotation** | **Yaw:** Turn horizontally | **Pitch:** Tilt forward / backward |
| 🔃 **2. Roll** | **Roll:** Tilt sideways | **Roll:** Tilt sideways |
| 🦴 **3. Joint Bend** | **Joint Yaw:** Rotate shin / forearm | **Joint Pitch:** Bend knee / elbow joint |
| 🦴 **4. Joint Tilt** | **Joint Roll:** Tilt shin / forearm | **Joint Roll:** Tilt shin / forearm |
| ↕️ **5. Vertical / Depth** | **Y-Axis:** Move up / down | **Z-Axis:** Move forward / back |
| ↔️ **6. Horizontal** | **X-Axis:** Move left / right | **X-Axis:** Move left / right |

> 💡 **Pro Tip (Sitting Pose):** Select the **Right Leg**, lift the thigh in **Mode 1** (`Shift+Scroll`), switch to **Mode 3**, and bend the knee down (`Shift+Scroll`). Repeat for the Left Leg!

---

## ⚡ Action Engine

Turn your NPCs into interactive server elements! Bind action chains executed when players click them:

* `[message] <text>` - Send a formatted chat message.
* `[console] <cmd>` - Execute a command from the console.
* `[player] <cmd>` - Make the player execute a command.
* `[server] <name>` - Transfer the player across BungeeCord/Velocity.
* `[title] <text>` - Display an on-screen title.

**Conditions:**
Stop execution if a condition isn't met:
* `[require_permission] <perm>` - Requires a specific permission.
* `[require_money] <amount>` - Requires Vault economy balance.
* `[cooldown] <time>` - Prevent spam (e.g., `5s`, `10m`, `24h`, `7d`).

---

## 💻 Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/coconpc spawn <id> <skin>` | Spawns a new NPC. | `CocoNPC.spawn` |
| `/coconpc edit <id>` | Opens the visual editor. | `CocoNPC.npc.edit` |
| `/coconpc tp <id>` | Teleports to the NPC. | `CocoNPC.tp` |
| `/coconpc list` | Lists all active NPCs. | `CocoNPC.list` |
| `/coconpc action <id> ...` | Manages click actions. | `CocoNPC.action` |
| `/coconpc pose <id> <pose>` | Applies a preset pose. | `CocoNPC.pose` |
| `/coconpc resize <id> <size>` | Resizes an NPC. | `CocoNPC.resize` |
| `/coconpc item <id> <hand>` | Equips held item to the NPC. | `CocoNPC.item` |
| `/coconpc reload` | Reloads configuration. | `CocoNPC.reload` |

---

## 🛠️ Building & Installation

### Project Structure
- `plugin/` - The Java source code for the PaperMC plugin. (Use `./gradlew clean build` here)
- `web/` - The Next.js documentation website. (Use `npm install` & `npm run dev` here to start the page locally)

### Requirements
- **PaperMC Server:** `1.21` or newer.
- **Java:** `21` or newer.
- **PacketEvents 2.13.0+:** Required for packet rendering.

### Setup
1. Download `CocoNPC-1.0.0.jar` and drop it into your `plugins` folder.
2. Start the server to generate `plugins/CocoNPC/config.yml`.
3. Get a free API key at [mineskin.org/account](https://mineskin.org/account) and add it to your `config.yml` (or use `/coconpc setkey <key>`). **This is required to generate custom skins!**
4. Drop your `.png` skins in `plugins/CocoNPC/skins/` and run `/coconpc preload`.

### Compile from Source
```bash
./gradlew clean build
```
The compiled artifact will be located in `build/libs/CocoNPC-1.0.0.jar`.

---
<div align="center">
<i>Crafted with ❤️ for modern Minecraft servers.</i>
</div>
