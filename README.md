# NormalTreeCapitator

Cut down an entire connected tree — logs, leaves, nether stems, mushrooms, and more — with a single break. NormalTreeCapitator is a focused tree capitator plugin built for survival servers that want reliable performance, deep configuration, and broad platform support without extra bloat.

Many tree capitator plugins were written before Folia, before large-scale async breaking, or with fixed block lists you cannot change without editing code. NormalTreeCapitator takes a different approach: every block group, tool, limit, and message is YAML-driven, large chains are broken in timed waves to reduce lag spikes, and scheduling works correctly on **Paper**, **Folia**, **Spigot**, and **Bukkit**.

**Authors:** Agentsix1, Cristichi  
**Discord:** [Normal Survival](https://discord.normalsurvival.com)

---

## Table of contents

- [Why use NormalTreeCapitator?](#why-use-normaltreecapitator)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
  - [Global settings](#global-settings)
  - [Block groups](#block-groups)
  - [Adding your own groups](#adding-your-own-groups)
  - [Messages](#messages)
  - [Player data](#player-data)
- [How it works](#how-it-works)
- [Building from source](#building-from-source)
- [Server files](#server-files)
- [Troubleshooting](#troubleshooting)

---

## Why use NormalTreeCapitator?

Tree capitators all do the same basic job, but the details matter on a live server:

| Concern | How NormalTreeCapitator handles it |
|--------|-------------------------------------|
| **Folia / multi-threaded servers** | Uses region- and entity-aware schedulers on Paper/Folia; falls back to the standard Bukkit scheduler on Spigot/Bukkit. |
| **Lag on huge trees** | Chains above `async-start` break in waves (`blocks-per-tick`) instead of all at once. |
| **Different tree types** | Separate configurable **groups** — trees, mushrooms, or anything you define — each with their own blocks, tools, and limits. |
| **Player choice** | Per-player toggle with saved preferences. |
| **Server customization** | Full `config.yml` and `messages.yml` editing; reload without restart. |
| **Survival fairness** | Optional axe requirement, tool durability, sneak-to-activate, replant, and sapling protection. |

The plugin does one thing and exposes the knobs admins actually need.

---

## Features

- **Flood-fill tree breaking** — connected logs, leaves, stems, hyphae, and custom blocks break together.
- **Multiple block groups** — e.g. `Trees` and `Other` (mushrooms) by default; add unlimited custom groups.
- **All major wood types** — overworld logs/wood (including stripped), nether stems/hyphae, leaves, azalea, huge mushrooms, nether wart, shroomlight.
- **Async bulk breaking** — large chains spread across ticks to protect TPS.
- **Tool rules** — require an axe, damage durability (with Unbreaking support), optional “break axe” behavior, unbreakable tool support.
- **Replant** — optionally replant saplings/fungi on log break; optional invincible replant protection.
- **Per-player toggle** — `/tc toggle` with persistent `playerdata/` storage.
- **Custom messages** — every command response and prefix in `messages.yml` with `&` color codes.
- **Cross-platform** — Paper, Folia, Spigot, Bukkit (Minecraft **1.20+**).
- **Lightweight** — no dependencies beyond the server API; bStats metrics included.

---

## Requirements

- **Minecraft:** 1.20 or newer
- **Server software:** Paper, Folia, Spigot, or Bukkit
- **Java:** 17+ (Java 21 recommended for newer Paper versions)

---

## Installation

1. Download or build `NormalTreeCapitator-1.0.0-SNAPSHOT.jar` (see [Building from source](#building-from-source)).
2. Place the JAR in your server's `plugins/` folder.
3. Start or restart the server.
4. Edit `plugins/NormalTreeCapitator/config.yml` and `messages.yml` as needed.
5. Run `/tc reload` to apply changes without a full restart.

On first run the plugin creates:

```
plugins/NormalTreeCapitator/
├── config.yml
├── messages.yml
└── playerdata/
    └── <uuid>.yml
```

---

## Quick start

1. Give players `normaltreecapitator.use` (enabled by default).
2. Hold an axe and break any log in a tree.
3. Connected blocks in the same **group** break together.
4. Players can disable it for themselves with `/tc toggle`.

If `must-sneak` is `true` in config (default in the shipped config), players must be sneaking while breaking.

---

## Commands

All commands use `/tc`. Aliases: `/treecapitator`, `/treecap`.

| Command | Permission | Description |
|---------|------------|-------------|
| `/tc help` | `normaltreecapitator.help` | Show available subcommands |
| `/tc toggle` | `normaltreecapitator.toggle` | Toggle tree capitator for yourself |
| `/tc toggle <player>` | `normaltreecapitator.toggle.others` | Toggle for another online player |
| `/tc reload` | `normaltreecapitator.reload` | Reload `config.yml` and `messages.yml` |

**Examples**

```
/tc help
/tc toggle
/tc toggle Steve
/tc reload
```

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `normaltreecapitator.*` | op | All permissions below |
| `normaltreecapitator.use` | `true` | Break trees with tree capitator |
| `normaltreecapitator.toggle` | `true` | Toggle for yourself |
| `normaltreecapitator.toggle.others` | `false` | Toggle for other players |
| `normaltreecapitator.reload` | `false` | Reload config files |
| `normaltreecapitator.admin` | `false` | Break protected replanted saplings |
| `normaltreecapitator.help` | `true` | View `/tc help` |

**LuckPerms example**

```
/lp group default permission set normaltreecapitator.use true
/lp group default permission set normaltreecapitator.toggle true
/lp group staff permission set normaltreecapitator.reload true
```

---

## Configuration

Config file: `plugins/NormalTreeCapitator/config.yml`  
Reload: `/tc reload`

### Global settings

These apply to every group unless a group overrides them.

```yaml
defaults:
  enabled: true          # New players start with tree capitator on

settings:
  max-chain: 1000        # Max blocks per chain (-1 = unlimited)
  search-radius: 1       # Connectivity range between blocks (1–5)
  must-sneak: true       # Require sneaking to activate
  need-tool: true        # Require a valid tool from the group's tools list
  damage-tool: true      # Apply durability loss per block broken
  break-tool: true       # Remove axe when durability runs out (false = leave at 1 durability)
  replant: false         # Replant saplings/fungi when breaking logs
  invincible-replant: false  # Protect replanted saplings until admin breaks them
  async-start: 100       # Chains larger than this use wave breaking
  blocks-per-tick: 100   # Blocks broken per tick during wave breaking
```

| Setting | What it does |
|---------|----------------|
| `max-chain` | Caps how many blocks one activation can break. Prevents runaway chains. |
| `search-radius` | How far the flood-fill looks for the next block. `1` = touching (26 neighbors). Higher values connect blocks farther apart. |
| `must-sneak` | When `true`, only works while the player is sneaking. Good for preventing accidental caps. |
| `need-tool` | When `true`, the held item must be in the group's `tools` list. |
| `damage-tool` | When `true`, axe durability is consumed (respects Unbreaking and unbreakable tools). |
| `break-tool` | When `true` and durability reaches zero, the axe is destroyed. When `false`, it stays at 1 durability remaining. |
| `replant` | Replaces broken logs with the correct sapling/fungus on valid ground. |
| `invincible-replant` | Replanted saplings cannot be broken except by players with `normaltreecapitator.admin`. |
| `async-start` | Chains bigger than this threshold use timed waves instead of instant breaking. |
| `blocks-per-tick` | How many blocks each wave breaks. Lower = smoother TPS, slower completion. |

### Block groups

Groups are the core of the config. Each group defines:

- Which **blocks** chain together
- Which **tools** activate the capitator
- Optional per-group `max-chain` and `search-radius`

**Important:** blocks only chain within the **same group**. Breaking an oak log will not chain into mushroom blocks, because they are in different groups.

The default config includes:

| Group | Purpose |
|-------|---------|
| `Trees` | Overworld/nether wood, leaves, nether wart, shroomlight |
| `Other` | Huge mushroom stems and caps |

Block names accept any of these formats:

```
minecraft:oak_log
oak_log
OAK_LOG
```

Unknown block names on older Minecraft versions are **skipped with a console warning** — the rest of the config still loads.

### Adding your own groups

Copy an existing group block, rename it, and edit the lists:

```yaml
groups:
  Bamboo:
    max-chain: 250
    search-radius: 1
    blocks:
      - minecraft:bamboo_block
      - minecraft:bamboo
    tools:
      - minecraft:iron_axe
      - minecraft:diamond_axe
      - minecraft:netherite_axe
```

You can add as many groups as you want. Group names are labels for your own organization — they are not shown to players.

**Per-group overrides**

```yaml
  Trees:
    max-chain: 500       # Trees can break up to 500 blocks
    search-radius: 1
    blocks:
      - minecraft:oak_log
      # ...
```

If `max-chain` or `search-radius` is omitted, the global `settings` values are used.

### Messages

File: `plugins/NormalTreeCapitator/messages.yml`

Every player-facing string can be customized. Uses standard `&` color codes and `{placeholder}` replacements.

```yaml
prefix: "&9[NormalTreeCapitator]&f "

toggle-self: "{feature} {state}"
player-not-found: "&cPlayer not found: &6{player}"
sapling-protected: "&cThis replanted sapling is protected."
```

| Key | Placeholders | Used when |
|-----|--------------|-----------|
| `usage` | `{label}`, `{usage}` | Invalid command usage |
| `toggle-self` | `{feature}`, `{state}` | Player toggles themselves |
| `toggle-other-sender` | `{feature}`, `{state}`, `{target}` | Staff toggles another player |
| `toggle-other-target` | `{feature}`, `{state}`, `{sender}` | Message sent to the target |
| `help-toggle` | `{label}`, `{feature}` | Help line for toggle |
| `feature-treecapitator` | — | Display name in toggle messages |
| `state-enabled` / `state-disabled` | — | Toggle state text |

Reload messages with `/tc reload`.

### Player data

Per-player toggle state is stored in:

```
plugins/NormalTreeCapitator/playerdata/<uuid>.yml
```

```yaml
enabled: true
```

New players use `defaults.enabled` from `config.yml` until they toggle or a file is created.

---

## How it works

1. A player breaks a block that belongs to a configured group.
2. The plugin checks permissions, toggle state, game mode (Survival/Adventure only), sneak requirement, and tool validity.
3. A flood-fill collects all connected blocks of the same group (within `search-radius` and `max-chain`).
4. If the chain is small (≤ `async-start`), blocks break immediately with per-block drops.
5. If the chain is large (> `async-start`), breaks run in waves of `blocks-per-tick`, merging drops at the origin block.
6. Tool durability is applied up front for large chains (matching vanilla order: damage first, then break).
7. Replant runs on individual logs when enabled, placing the correct sapling or fungus.

Scheduling is region-safe on Folia: entity work runs on the player’s entity scheduler, block changes run on the region that owns each block.

---

## Building from source

**Prerequisites:** Java 17+, Maven 3.8+

```bash
cd NormalTreeCapitator
mvn clean package
```

Output JAR:

```
target/NormalTreeCapitator-1.0.0-SNAPSHOT.jar
```

This is a shaded JAR (bStats is relocated internally). Copy it to `plugins/` — no extra libraries needed.

---

## Server files

| File / folder | Purpose |
|---------------|---------|
| `config.yml` | Settings, block groups, tools |
| `messages.yml` | Prefix, command messages, toggle text |
| `playerdata/` | Per-player enabled/disabled state |

Deleting `config.yml` or `messages.yml` and restarting regenerates defaults from the JAR (back up custom edits first).

---

## Troubleshooting

**Tree capitator does nothing**

- Check `normaltreecapitator.use` permission.
- Run `/tc toggle` — player may have it disabled.
- If `must-sneak: true`, the player must be sneaking.
- Confirm the block is listed in a group in `config.yml`.
- Confirm the held item is in that group's `tools` list (if `need-tool: true`).

**Only one block breaks**

- The broken block may not be connected to others within `search-radius`.
- `max-chain` may be too low.
- Another plugin may be cancelling `BlockBreakEvent`.

**Unknown block warnings on startup**

- A block ID in config does not exist on your Minecraft version (e.g. `pale_oak_log` on 1.20). Remove it or upgrade; other blocks still work.

**Config changes not applying**

- Run `/tc reload` or restart the server.
- Edit the file in `plugins/NormalTreeCapitator/`, not the copy inside the JAR.

**Lag on huge trees**

- Lower `blocks-per-tick` (e.g. `50`).
- Lower `async-start` so wave breaking kicks in sooner.
- Lower `max-chain` to cap chain size.

---

## License & metrics

This plugin includes [bStats](https://bstats.org/) for anonymous usage statistics (plugin ID: **32277**). No player data is sent through bStats.

---

*Questions or feedback? Join [Normal Survival on Discord](https://discord.normalsurvival.com).*
