# NormalTreeCapitator

**Chop one log. Break the whole tree.**

NormalTreeCapitator lets players cut down entire connected trees — logs, leaves, nether wood, mushrooms, and more — with a single axe swing. It is built for survival servers that want reliable performance on **Paper**, **Folia**, **Spigot**, and **Bukkit**, with every rule exposed in YAML.

This document is the full reference for the plugin: commands, permissions, every config option, messages, behavior, and troubleshooting.

**Authors:** [Agentsix1](https://github.com/agentsix1) · Cristichi  
**Original inspiration:** [Cristichi's Tree Capitator](https://www.curseforge.com/minecraft/bukkit-plugins/cristichis-tree-capitator)  
**Discord:** [Normal Survival](https://discord.normalsurvival.com)  
**Issues:** [GitHub Issues](https://github.com/agentsix1/NormalTreeCapitator/issues)

### Downloads

| Source | Link |
|--------|------|
| **Modrinth** (recommended) | [modrinth.com/plugin/normal-tree-capitator](https://modrinth.com/plugin/normal-tree-capitator/versions) |
| **GitHub** | [github.com/agentsix1/NormalTreeCapitator](https://github.com/agentsix1/NormalTreeCapitator) |
| **Version feed** | [Pastebin raw](https://pastebin.com/raw/nc6CbGem) (`version\|download url` — used by in-game update checks) |
| **Changelog** | [1.0.5.md](1.0.5.md) · [1.0.4.md](1.0.4.md) · [1.0.3.md](1.0.3.md) |

**Current release: [1.0.5](1.0.5.md)** — structure protection, live `/tc status`, languages (`EN-us` / `EN-gb` / `EN-sg` / `DE-de` / `ES-es` / `PT-br`), smarter Pastebin update polling.

---

## Table of contents

- [Overview](#overview)
- [What's new in 1.0.5](#whats-new-in-105)
- [What's new in 1.0.4](#whats-new-in-104)
- [Upgrading to 1.0.4](#upgrading-to-104)
- [What's new in 1.0.3](#whats-new-in-103)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Commands](#commands)
  - [Update checks](#update-checks)
- [Permissions](#permissions)
- [Configuration reference](#configuration-reference)
  - [File locations](#file-locations)
  - [language](#language)
  - [defaults.enabled](#defaultsenabled)
  - [Global settings](#global-settings)
  - [block-damages](#block-damages)
  - [Block groups](#block-groups)
  - [Adding custom groups](#adding-custom-groups)
- [Messages reference](#messages-reference)
- [Player data](#player-data)
- [How it works](#how-it-works)
  - [Activation checks](#activation-checks)
  - [Sneak behavior](#sneak-behavior)
  - [Flood fill & connectivity](#flood-fill--connectivity)
  - [Sync vs async breaking](#sync-vs-async-breaking)
  - [Drops](#drops)
  - [Tool durability](#tool-durability)
  - [Replant system](#replant-system)
  - [Protection plugins](#protection-plugins)
  - [Scheduling (Folia)](#scheduling-folia)
- [Default content](#default-content)
- [Building from source](#building-from-source)
- [Troubleshooting](#troubleshooting)
- [Metrics](#metrics)
- [Changelog](#changelog)
- [Links](#links)

---

## Overview

When a player breaks a block that belongs to a configured **group**, NormalTreeCapitator:

1. Finds every connected block in that same group (flood fill).
2. Cancels the vanilla break and breaks the whole chain for the player.
3. Applies tool rules, protection checks, and optional replant.

Key design goals:

| Goal | How it is handled |
|------|-------------------|
| **Folia-safe** | Block reads and breaks run on the correct region thread; no sync chunk loads across regions. |
| **TPS-friendly** | Large chains break in timed **waves** instead of all at once. |
| **Flexible** | Unlimited YAML **groups** — trees, mushrooms, bamboo, or anything you define. |
| **Tool tiers** | Same woods can live in multiple groups; the held tool picks which chain runs. |
| **Permission tiers** | Optional `permission:` on groups and damage sections (e.g. VIP lower durability / bigger chains). |
| **Player choice** | Per-player toggle saved to disk; staff can check status online or offline. |
| **Claim-aware** | Each block fires a break check; protected blocks are skipped, the rest still break. |
| **Structure protection** | Skips tree-cap on player builds and world structures (houses, villages, log walls). |
| **Languages** | Server default in `config.yml` / `/tc language server`; optional personal `/tc language` (not in `user`). Bundled: `EN-us`, `EN-gb`, `EN-sg`, `DE-de`, `ES-es`, `PT-br`. |
| **Fair replant** | Optional auto-replant from real sapling drops; 0-cost leaves break with the tree only when every trunk in the chain is cut. |
| **Update alerts** | Ops / `normaltreecapitator.admin` get a clickable download link when a newer release is published. |

---

## What's new in 1.0.5

Full notes: **[1.0.5.md](1.0.5.md)**

| Feature | Summary |
|---------|---------|
| **Structure protection** | Tree cap only runs on natural-looking trees (grounded stump + real foliage/canopy); builds fall back to vanilla break. |
| **Structure cleanup** | Orphan leaf-only / log-only leftovers can still be tree-capped (`structure-cleanup`). |
| **Command UX** | `/tc` toggle state · `/tc <player>` staff lookup · `/tc status` live break progress · `/tc help` lists only what you can use. |
| **`admin.state`** | Staff lookup permission renamed from `admin.status` → `normaltreecapitator.admin.state`. |
| **User pack commands** | Each default-player command has its own permission node (also granted by `normaltreecapitator.user`). |
| **`/tc version`** | Admin-only (`normaltreecapitator.version`, default op; included in `admin` pack). |
| **Languages** | Chat strings in `languages/` (`EN-us`, `EN-gb`, `EN-sg`, `DE-de`, `ES-es`, `PT-br`); server default via `language:` / `/tc language server`; optional personal `/tc language` (not in `user`). |
| **`/tc structure-protection`** | Per-player opt-out (`normaltreecapitator.structure-protection`, not in `user`). |
| **30-minute Pastebin poll** | Re-checks the version feed; notifies console + staff when a new remote version appears. |
| **Join + 3-hour reminders** | Ops / `normaltreecapitator.admin` get a fresh check on join; recurring reminder every 3 hours while outdated. |

### Default user pack (`normaltreecapitator.user`)

| Command / feature | Permission node | Default |
|-------------------|-----------------|---------|
| Tree cap when breaking | `normaltreecapitator.use` | `true` |
| `/tc` (toggle state) | `normaltreecapitator.status` | `true` |
| `/tc status` (break progress) | `normaltreecapitator.progress` | `true` |
| `/tc help` | `normaltreecapitator.help` | `true` |
| `/tc toggle` | `normaltreecapitator.toggle` | `true` |

Grant `normaltreecapitator.user` for all of the above, or set individual nodes.

## What's new in 1.0.4

Full notes: **[1.0.4.md](1.0.4.md)**

| Feature | Summary |
|---------|---------|
| **`normaltreecapitator.user` / `admin.*`** | Permission packs; optional group & damage `permission:` gates. |
| **Group / damage permissions** | Optional `permission: vip` → `normaltreecapitator.group.vip` / `damage.vip`. |

## Upgrading to 1.0.4

1. Replace the jar with `NormalTreeCapitator-1.0.4.jar` and **restart** the server (new permission nodes).
2. Update permission plugins if you used the old nodes:

| Old (1.0.3) | New (1.0.4) |
|-------------|-------------|
| `normaltreecapitator.reload` | `normaltreecapitator.admin.reload` |
| `normaltreecapitator.status` (if used for others) | Self: `normaltreecapitator.status` · Others: `normaltreecapitator.admin.status` (renamed to `admin.state` in 1.0.5) |
| `normaltreecapitator.toggle.others` | `normaltreecapitator.admin.toggle.others` |

Or grant `normaltreecapitator.admin` / `normaltreecapitator.user` instead of listing children.

3. Run `/tc reload` to merge new message keys into an existing language file under `languages/`.
4. Update the [Pastebin version feed](https://pastebin.com/raw/nc6CbGem) to `v1.0.4|https://modrinth.com/plugin/normal-tree-capitator/versions` when you publish.

## What's new in 1.0.3

Full notes: **[1.0.3.md](1.0.3.md)**

| Feature | Summary |
|---------|---------|
| **Tool-aware groups** | Group is chosen by block **and** held tool. Overlapping woods across tiers (stone vs iron) work as expected. |
| **Config ownership** | Your `groups:` and `block-damages:` are never restored from the jar on `/tc reload`. |
| **Custom damage sections** | Any named section under `block-damages` (`logs`, `mushrooms`, `heavy_logs`, …). |
| **Low-durability replant** | With `break-tool: false`, 0-cost foliage breaks only when the budget covers every trunk (no bare leftover logs); no free saplings are spawned. |
| **`/normaltreecap` alias** | Same command as `/tc` / `/treecap` / `/treecapitator`. |
| **`/tc version`** | Shows installed version; if outdated, shows a clickable download URL. |
| **Pastebin update feed** | Staff notified on enable, join, and periodically when behind (improved polling in **1.0.5**). |

---

## Requirements

| | Minimum | Recommended |
|---|---------|-------------|
| **Minecraft** | 1.20+ | Latest stable for your server |
| **Server** | Paper, Folia, Spigot, or Bukkit | Paper or Folia |
| **Java** | 17 | 21 on newer Paper builds |

**Game modes:** Survival and Adventure only. Creative and Spectator are ignored.

---

## Installation

1. Download **1.0.5** from [Modrinth](https://modrinth.com/plugin/normal-tree-capitator/versions) (or [GitHub](https://github.com/agentsix1/NormalTreeCapitator), or [build from source](#building-from-source)).
2. Place `NormalTreeCapitator-1.0.5.jar` in your server's `plugins/` folder.
3. Start or restart the server.
4. Edit `plugins/NormalTreeCapitator/config.yml` and `languages/<language>.yml` if needed.
5. Run `/tc reload` to apply config/message changes without a restart.

> **Note:** Code updates require a **server restart**. `/tc reload` only reloads YAML files.

On first run the plugin creates:

```
plugins/NormalTreeCapitator/
├── config.yml
├── languages/
│   ├── EN-us.yml
│   ├── EN-gb.yml
│   ├── EN-sg.yml
│   ├── DE-de.yml
│   ├── ES-es.yml
│   └── PT-br.yml
└── playerdata/
    └── <uuid>.yml
```

On load you should see a line like:

```
[TreeCap] config .../config.yml language=EN-us must-sneak=true debug=false async-start=100 replant=true
```

---

## Quick start

**For players**

1. Hold an axe (or whatever tools your group allows).
2. Break a log or leaf in a configured tree.
3. The connected tree in that **group** breaks together.
4. Use `/tc toggle` to turn tree cap on or off for yourself.

**For admins**

1. Players get basics via `normaltreecapitator.user` (default: true). Staff get `normaltreecapitator.admin`.
2. Adjust `must-sneak`, `replant`, `language`, groups, and damage (including optional VIP `permission:`) in `config.yml`.
3. Optionally grant `normaltreecapitator.language` so players can `/tc language <code>` for themselves.
4. Run `/tc reload` after YAML edits (restart after jar updates).
5. Use `/tc <player>` to check toggle state for online or offline players.

If `must-sneak: true` (shipped default), players must **hold sneak** while breaking to activate tree cap. See [Sneak behavior](#sneak-behavior) for the full matrix.

---

## Commands

Primary command: **`/tc`**  
Aliases: `/treecapitator`, `/treecap`, `/normaltreecap`

| Command | Permission | Pack | Description |
|---------|------------|------|-------------|
| `/tc` | `normaltreecapitator.status` | `user` | View your tree capitator toggle state |
| `/tc status` | `normaltreecapitator.progress` | `user` | Live progress of your current tree break (blocks done / total, %, rough ETA) |
| `/tc help` | `normaltreecapitator.help` | `user` | List only the commands you have permission to use |
| `/tc toggle` | `normaltreecapitator.toggle` | `user` | Toggle tree capitator for yourself |
| `/tc <player>` | `normaltreecapitator.admin.state` | `admin` | View a player's toggle state — online or offline |
| `/tc toggle <player>` | `normaltreecapitator.admin.toggle.others` | `admin` | Toggle for another online player |
| `/tc reload` | `normaltreecapitator.admin.reload` | `admin` | Reload `config.yml` and all language files |
| `/tc version` | `normaltreecapitator.version` | `admin` | Show installed version; if outdated, show a clickable download link |
| `/tc language <code>` | `normaltreecapitator.language` | — | Set your personal chat language (not in `user`; saved in playerdata) |
| `/tc language server <code>` | `normaltreecapitator.admin.language` | `admin` | Set the server default chat language (writes `language:` in `config.yml`) |
| `/tc structure-protection` | `normaltreecapitator.structure-protection` | — | Toggle whether structure protection applies to you (not in `user`) |

**Examples**

```
/tc
/tc Steve
/tc status
/tc help
/tc language EN-us
/tc language server DE-de
/normaltreecap version
/tc toggle
/tc toggle Steve
/tc structure-protection
/tc reload
```

### Update checks

The plugin reads the latest release from [Pastebin raw](https://pastebin.com/raw/nc6CbGem) (`version|download url`).

| When | What happens |
|------|----------------|
| **Every 30 minutes** | Re-fetches Pastebin. If a newer version appears (or the feed changed), console + online ops / `normaltreecapitator.admin` are notified |
| **Plugin enable** | Immediate Pastebin check; notify console + online staff if outdated |
| **Player join** | Fresh Pastebin check for that player if they are op or have `normaltreecapitator.admin` |
| **Every 3 hours** | Reminder to console + online staff while still outdated |
| **`/tc version`** | Fresh check for whoever ran the command (staff/op by default; clickable link if outdated) |

---

## Permissions

### Packs

| Permission | Default | Includes |
|------------|---------|----------|
| `normaltreecapitator.*` | op | `user` + `admin` + structure-protection + language |
| `normaltreecapitator.user` | `true` | `use`, `toggle`, `status`, `progress`, `help` |
| `normaltreecapitator.admin` | op | `admin.state`, `admin.reload`, `admin.language`, `admin.toggle.others`, `version`, plus protected saplings & update alerts |

### Individual nodes

| Permission | Default | Description |
|------------|---------|-------------|
| `normaltreecapitator.use` | `true` | Activate tree capitator when breaking blocks |
| `normaltreecapitator.toggle` | `true` | `/tc toggle` |
| `normaltreecapitator.status` | `true` | `/tc` — view your toggle state |
| `normaltreecapitator.progress` | `true` | `/tc status` — live chain break progress |
| `normaltreecapitator.help` | `true` | `/tc help` |
| `normaltreecapitator.version` | op | `/tc version` |
| `normaltreecapitator.structure-protection` | `false` | `/tc structure-protection` (not in `user`) |
| `normaltreecapitator.language` | `false` | `/tc language <code>` personal language (not in `user`) |
| `normaltreecapitator.admin.state` | op | `/tc <player>` toggle lookup |
| `normaltreecapitator.admin.reload` | op | `/tc reload` |
| `normaltreecapitator.admin.language` | op | `/tc language server <code>` |
| `normaltreecapitator.admin.toggle.others` | op | `/tc toggle <player>` |
| `normaltreecapitator.group.<name>` | — | From `groups.*.permission` (e.g. `vip` → `normaltreecapitator.group.vip`) |
| `normaltreecapitator.damage.<name>` | — | From `block-damages.*.permission` (e.g. `vip` → `normaltreecapitator.damage.vip`) |

Config `permission: vip` becomes `normaltreecapitator.group.vip` or `normaltreecapitator.damage.vip`. A value that already contains `.` is used as a full permission node.

**LuckPerms examples**

```
/lp group default permission set normaltreecapitator.user true
/lp group default permission set normaltreecapitator.language true
/lp group vip permission set normaltreecapitator.group.vip true
/lp group vip permission set normaltreecapitator.damage.vip true
/lp group staff permission set normaltreecapitator.admin true
```

---

## Configuration reference

**File:** `plugins/NormalTreeCapitator/config.yml`  
**Reload:** `/tc reload` (requires `normaltreecapitator.admin.reload`)

Block and tool IDs accept any of these formats:

```
minecraft:oak_log
oak_log
OAK_LOG
```

Unknown block names on older Minecraft versions are **skipped with a console warning** — the rest of the config still loads.

---

### File locations

| Path | Purpose |
|------|---------|
| `config.yml` | All gameplay settings, groups, block-damages, server language |
| `languages/<code>.yml` | Chat strings and colors for that language |
| `playerdata/<uuid>.yml` | Per-player toggle, structure-protection opt-out, personal language |

Always edit files under `plugins/NormalTreeCapitator/`, not copies inside the JAR.

---

### language

```yaml
# Available languages: EN-us (English US), EN-gb (English UK), EN-sg (English Singapore),
# DE-de (German), ES-es (Spanish), PT-br (Brazilian Portuguese)
language: EN-us
```

| | |
|--|--|
| **Default** | `EN-us` |
| **Meaning** | Server default language under `languages/` (players without a personal language use this) |
| **Bundled** | `EN-us`, `EN-gb`, `EN-sg`, `DE-de`, `ES-es`, `PT-br` |
| **Custom** | Add your own `languages/XX-yy.yml` and set `language` to that name (without `.yml`) |
| **Players** | With `normaltreecapitator.language`, `/tc language <code>` saves a personal override in `playerdata/` |
| **Staff** | `/tc language server <code>` (or `language:` in config) changes the server default |

If the chosen file is missing, the plugin falls back to `EN-us` and logs a warning. An old root `messages.yml` is moved to `languages/EN-us.yml` once on upgrade if English was not extracted yet.

---

### defaults.enabled

```yaml
defaults:
  enabled: true
```

| Value | Meaning |
|-------|---------|
| `true` | New players start with tree capitator **on** until they `/tc toggle` |
| `false` | New players start with tree capitator **off** |

Once a player toggles, their choice is saved in `playerdata/` and overrides this default.

---

### Global settings

```yaml
settings:
  max-chain: 1000
  search-radius: 1
  must-sneak: true
  need-tool: true
  damage-tool: true
  break-tool: false
  merge-item-drops: true
  cooldown-ticks: 0
  replant: true
  invincible-replant: false
  replant-consume-saplings: true
  debug: false
  async-start: 100
  blocks-per-tick: 100
  async-delay: 1
  structure-protection: true
  structure-cleanup: true
```

These apply to **every group** unless a group overrides `max-chain` or `search-radius`.

#### max-chain

| | |
|---|---|
| **Type** | Integer |
| **Default** | `1000` |
| **Special** | `-1` = unlimited |

Maximum blocks one tree-cap activation can break. Prevents runaway chains on huge builds or misconfigured groups.

Groups can override this per group (see [Block groups](#block-groups)).

---

#### search-radius

| | |
|---|---|
| **Type** | Integer |
| **Range** | `1` – `5` |
| **Default** | `1` |

How far the flood fill looks for the next connected block, measured in **Chebyshev** distance (a cube around each block).

| Value | Meaning |
|-------|---------|
| `1` | Full 26-neighbor adjacency (faces, edges, corners) — typical for trees |
| `2`+ | Blocks farther apart still count as connected — use carefully |

Groups can override this per group.

---

#### must-sneak

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

Controls **when** tree capitator activates relative to the shift key.

| Value | Standing | Sneaking |
|-------|----------|----------|
| `true` | Vanilla single-block break | Tree cap |
| `false` | Tree cap | Vanilla single-block break |

See [Sneak behavior](#sneak-behavior) for details. Shift state is tracked from toggle events so it stays accurate on Folia.

---

#### need-tool

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

| Value | Behavior |
|-------|----------|
| `true` | Held item must be an **axe** listed in the group's `tools` list |
| `false` | Any item can trigger tree cap (still must pass other checks) |

---

#### damage-tool

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

| Value | Behavior |
|-------|----------|
| `true` | Axe durability is consumed per block (see [block-damages](#block-damages)); Unbreaking is rolled per damage point |
| `false` | No durability loss from tree cap |

Unbreakable tools (vanilla tag) never lose durability regardless of this setting.

---

#### break-tool

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `false` |

Only matters when `damage-tool: true`.

| Value | Behavior |
|-------|----------|
| `true` | Axe is destroyed when durability reaches zero |
| `false` | Chain is **capped** so the axe keeps at least **1 durability** — it never breaks from tree cap alone |

Works together with [block-damages](#block-damages) and `ChainLimiter` logic.

---

#### merge-item-drops

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

| Value | Behavior |
|-------|----------|
| `true` | All item drops from the chain spawn as **one pile** at the origin block (center of first broken block) |
| `false` | Each block drops items at its own location as it breaks |

Experience is not modified by tree cap (blocks break with `exp = 0` on synthetic events; drops use vanilla `getDrops`).

---

#### cooldown-ticks

| | |
|---|---|
| **Type** | Integer (ticks) |
| **Default** | `0` |
| **Minimum** | `0` = no cooldown |

Ticks the player must wait before tree cap can activate again after a successful chain.  
20 ticks = 1 second.

Useful to prevent accidental double-activation or spam on busy servers.

---

#### replant

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

| Value | Behavior |
|-------|----------|
| `true` | After the **entire chain** finishes, stumps are replanted (see [Replant system](#replant-system)) |
| `false` | No automatic replanting |

Replant runs **after** all blocks break, not during the chain.

---

#### invincible-replant

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `false` |

Requires `replant: true`.

| Value | Behavior |
|-------|----------|
| `true` | Replanted saplings/fungi (and block below) are **protected** — normal players cannot break them |
| `false` | Replanted blocks behave like normal saplings |

Players with `normaltreecapitator.admin` can break protected saplings.

---

#### replant-consume-saplings

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

Requires `replant: true`.

| Value | Behavior |
|-------|----------|
| `true` | Each stump replant **consumes one matching sapling/fungus/propagule** from the tree's collected drops |
| `false` | Stumps are planted **for free** without consuming drops |

Prevents sapling duplication when `true`.

---

#### debug

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `false` |

| Value | Behavior |
|-------|----------|
| `true` | Writes detailed `[TreeCap]` lines to console / `latest.log` |
| `false` | Silent except errors and config load line |

Useful lines include `evaluate`, `START`, per-block `RESULT=BROKEN` / `SKIP`, and replant `REPLANT ... RESULT=PLANTED`. See [Troubleshooting](#troubleshooting).

---

#### async-start

| | |
|---|---|
| **Type** | Integer |
| **Default** | `100` |
| **Minimum** | `1` |

| Condition | Break mode |
|-----------|------------|
| Chain size **≤** `async-start` | **Sync** — all blocks break in one pass (still region-scheduled on Folia) |
| Chain size **>** `async-start` | **Async waves** — see below |

Lower values move more trees to wave breaking (smoother TPS, slightly longer total time).

---

#### blocks-per-tick

| | |
|---|---|
| **Type** | Integer |
| **Default** | `100` |
| **Minimum** | `1` |

During **async wave** breaking, how many blocks each wave attempts to break.

| Tuning | Effect |
|--------|--------|
| **Lower** (e.g. `25`–`50`) | Smoother TPS, slower tree completion |
| **Higher** (e.g. `150`+) | Faster completion, higher per-tick load |

---

#### async-delay

| | |
|---|---|
| **Type** | Integer (ticks) |
| **Default** | `1` |
| **Minimum** | `0` |

Ticks to wait **between async waves** after the first wave.

| Value | Behavior |
|-------|----------|
| `0` | Waves run back-to-back (still one wave per scheduling tick) |
| `1`+ | Pause between waves — spreads load over more server ticks |

---

#### structure-protection

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

When enabled, tree capitator **only** runs if the chain looks like a natural tree. Otherwise the break stays vanilla (single block).

| Check | Passes when |
|-------|-------------|
| Foliage | Leaves / wart / mushroom blocks present (skipped if `max-chain` capped) |
| Natural leaves | Most overworld leaves are non-persistent (not player-placed) |
| Grounded stump | A lowest trunk column sits on natural/plantable ground |
| Canopy | Some foliage sits above the lowest trunk |

Real trees next to houses still work — there is no adjacency block list. See **[1.0.5.md](1.0.5.md)**.

#### structure-cleanup

| | |
|---|---|
| **Type** | Boolean |
| **Default** | `true` |

Requires `structure-protection: true`. When a chain fails natural-tree checks, still allow tree-cap for:

- **Leaf-only** connected foliage (leftover canopy)
- **Log-only** stacks that only touch air, other logs in the chain, and the floor beneath them

#### Player override

| Command | Permission | Notes |
|---------|------------|-------|
| `/tc structure-protection` | `normaltreecapitator.structure-protection` | Toggle protection for yourself; disabled if server setting is off |

Not in `normaltreecapitator.user`.

Message: `structure-protected` in the active language file under `languages/`.

---

### block-damages

```yaml
block-damages:
  logs:
    damage: 1
    blocks:
      - minecraft:oak_log
      # ...
  leaves:
    damage: 0
    blocks:
      - minecraft:oak_leaves
      # ...
```

Defines how much **durability** each block type costs when `damage-tool: true`.

| Concept | Detail |
|---------|--------|
| **Structure** | Any number of named sections — labels are yours (`logs`, `leaves`, `mushrooms`, `heavy_logs`, …). Each needs `damage:` and a `blocks:` list |
| **damage** | Integer ≥ `0` — durability points lost per block broken |
| **permission** | Optional. `vip` → `normaltreecapitator.damage.vip`. Gated rules override open rules for the same blocks when the player has the permission |
| **Unlisted blocks** | Leaves, nether wart blocks, and shroomlight default to **0**; everything else defaults to **1** |
| **Unbreaking** | Rolled **per damage point**, same as vanilla |
| **Reload** | Your `block-damages:` section is never restored from the jar — deleting `logs` / renaming sections sticks after `/tc reload` |

**Example — VIP lower durability on the same logs:**

```yaml
block-damages:
  logs:
    damage: 2
    blocks:
      - minecraft:oak_log
      - minecraft:spruce_log
  logsvip:
    permission: vip   # → normaltreecapitator.damage.vip
    damage: 1
    blocks:
      - minecraft:oak_log
      - minecraft:spruce_log
  leaves:
    damage: 0
    blocks:
      - minecraft:oak_leaves
      - minecraft:spruce_leaves
```

When `break-tool: false`, the plugin stops the chain once the axe would drop below 1 durability, accounting for these costs in order (trunks first — see [How it works](#how-it-works)).

---

### Block groups

Groups are the core of tree capitator. Each group is a named section under `groups:` in config.

```yaml
groups:
  Trees:
    blocks:
      - minecraft:oak_log
      - minecraft:oak_leaves
    tools:
      - minecraft:iron_axe
      - minecraft:diamond_axe
```

#### Group fields

| Field | Required | Description |
|-------|----------|-------------|
| `blocks` | **Yes** | Block types that belong to this group and can chain together |
| `tools` | **Yes** | Items that can activate tree cap for this group (usually axes) |
| `permission` | No | Optional. `vip` → `normaltreecapitator.group.vip`. Gated groups are preferred when the player has the permission |
| `max-chain` | No | Overrides global `settings.max-chain` for this group only |
| `search-radius` | No | Overrides global `settings.search-radius` for this group only |

#### Group rules

1. **Same group only** — breaking oak log chains into oak leaves **only if both are in the same group's `blocks` list**.
2. **Different groups never chain** — `Trees` and `Other` (mushrooms) stay separate by default.
3. **Group names are labels** — not shown to players; use any YAML key you like (`Trees`, `stone`, `iron`, `Mushrooms`, `Bamboo`, etc.).
4. **Tool + block + permission select the group** — with `need-tool: true`, a matching group must list the block and tool. Permission-gated groups the player qualifies for win over open groups. The same block may appear in multiple groups with different tools or permissions.
5. **If `need-tool: false`** — tool lists are ignored for selection; permission gating still applies.

#### Default groups

| Group | Contents |
|-------|----------|
| **Trees** | Overworld logs/wood (incl. stripped), nether stems/hyphae, all major leaves, azalea, nether wart, shroomlight |
| **Other** | Huge mushroom stem and cap blocks |

Pale oak and other newer blocks are listed in config; on older servers missing those IDs, they are skipped with a warning.

---

### Adding custom groups

Copy an existing group, rename it, and edit the lists:

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

#### Tool tiers (overlapping blocks)

List the same woods in more than one group, with different `tools`. The held axe picks which group (and therefore which chain) applies:

```yaml
groups:
  stone:
    blocks:
      - minecraft:oak_log
      - minecraft:oak_leaves
      - minecraft:birch_log
      - minecraft:birch_leaves
    tools:
      - minecraft:stone_axe
  iron:
    blocks:
      - minecraft:oak_log
      - minecraft:oak_leaves
      - minecraft:birch_log
      - minecraft:birch_leaves
      - minecraft:dark_oak_log
      - minecraft:dark_oak_leaves
      - minecraft:jungle_log
      - minecraft:jungle_leaves
    tools:
      - minecraft:iron_axe
```

With an iron axe, breaking oak uses the `iron` group and can chain into dark oak. With a stone axe, the same oak only chains oak/birch.

#### Permission-gated groups (VIP, ranks, …)

```yaml
groups:
  Trees:
    blocks: [minecraft:oak_log, minecraft:oak_leaves]
    tools: [minecraft:iron_axe]
  TreesVip:
    permission: vip   # → normaltreecapitator.group.vip
    blocks: [minecraft:oak_log, minecraft:oak_leaves, minecraft:dark_oak_log, minecraft:dark_oak_leaves]
    tools: [minecraft:iron_axe]
```

Players with `normaltreecapitator.group.vip` use `TreesVip` when it matches; everyone else falls back to open groups like `Trees`.

**Tips**

- Keep mushrooms in their own group if you don't want them chaining with normal trees.
- Use a higher `search-radius` only when blocks in your build are intentionally spaced apart.
- Set a lower `max-chain` on groups that should cap smaller (e.g. bamboo farms).
- For tool tiers without permissions, do not put the same tool on two open groups that share a block — gated groups are preferred, then the first open match.
- Your `groups:` section is never restored from the jar on reload — deleting or renaming `Trees` / `Other` is permanent until you add them back yourself.

After editing, run `/tc reload`.

---

## Messages reference

**Folder:** `plugins/NormalTreeCapitator/languages/`  
**Server default:** set by `language:` in `config.yml` (default `EN-us`) or `/tc language server <code>`  
**Personal override:** `/tc language <code>` with `normaltreecapitator.language` (saved in playerdata; not in `user`)  
**Bundled:** `EN-us.yml` (English US), `EN-gb.yml` (English UK), `EN-sg.yml` (English Singapore), `DE-de.yml` (German), `ES-es.yml` (Spanish), `PT-br.yml` (Brazilian Portuguese)  
**Reload:** `/tc reload` (requires `normaltreecapitator.admin.reload`)  
**Colors:** Standard `&` color codes (e.g. `&a` green, `&c` red)

Every key is prefixed automatically with `prefix` when sent to players. Missing keys fall back to the bundled English defaults. Chat to a player uses their personal language when set; otherwise the server default.

| Key | Placeholders | When it is used |
|-----|--------------|-----------------|
| `prefix` | — | Prepended to all messages |
| `usage` | `{label}`, `{usage}` | Invalid command syntax |
| `unknown-subcommand` | `{label}` | Unrecognized subcommand |
| `no-permission-toggle-others` | — | `/tc toggle <player>` without `admin.toggle.others` |
| `no-permission-status` | — | `/tc` without `status` |
| `no-permission-progress` | — | `/tc status` without `progress` |
| `no-permission-status-others` | — | `/tc <player>` without `admin.state` |
| `player-not-found` | `{player}` | Target not found (online or offline) |
| `toggle-self` | `{feature}`, `{state}` | Player toggles themselves |
| `toggle-other-sender` | `{feature}`, `{state}`, `{target}` | Staff toggles another player (sender view) |
| `toggle-other-target` | `{feature}`, `{state}`, `{sender}` | Staff toggles another player (target view) |
| `status-self` | `{feature}`, `{state}` | `/tc` for yourself |
| `status-other` | `{feature}`, `{state}`, `{target}`, `{presence}` | `/tc <player>` |
| `chain-status` | `{done}`, `{total}`, `{percent}`, `{remaining}`, `{eta}`, `{mode}` | `/tc status` during a break |
| `chain-status-idle` | — | `/tc status` with no active chain |
| `presence-online` / `presence-offline` | — | Inserted into `{presence}` |
| `only-players` | — | Console uses a self-only command without a player arg |
| `no-permission` | — | Missing toggle permission |
| `no-permission-reload` | — | Missing `admin.reload` |
| `reload-success` | — | After `/tc reload` |
| `help-language` | `{label}` | Help line for personal language |
| `help-language-server` | `{label}` | Help line for server language |
| `language-current` | `{language}`, `{languages}` | `/tc language` with no code |
| `language-set` | `{language}` | After a successful personal language change |
| `language-server-current` | `{language}`, `{languages}` | `/tc language server` with no code |
| `language-server-set` | `{language}` | After a successful server language change |
| `language-invalid` | `{language}`, `{languages}` | Unknown language code |
| `language-save-failed` | — | Failed to write server `language:` to config |
| `help-header` | `{label}` | `/tc help` header line |
| `help-toggle` | `{label}`, `{feature}` | Help line for toggle |
| `help-toggle-player` | `{label}` | Help line for toggle others |
| `help-self` / `help-player` / `help-status` | `{label}` | Help lines for `/tc`, `/tc <player>`, `/tc status` |
| `help-reload` | `{label}` | Help line for reload |
| `help-version` | `{label}` | Help line for version |
| `sapling-protected` | — | Breaking invincible replant without `admin` |
| `structure-protected` | — | Tree cap blocked by structure protection |
| `command-disabled-structure-protection` | — | `/tc structure-protection` while server setting is off |
| `structure-protection-self` | `{feature}`, `{state}` | Player toggles structure protection |
| `help-structure-protection` | `{label}` | Help line for structure-protection |
| `feature-structure-protection` | — | Display name in toggle message |
| `feature-treecapitator` | — | Display name in toggle messages |
| `feature-tree-capitator` | — | Lowercase feature name in help text |
| `state-enabled` | — | Text shown when feature is on |
| `state-disabled` | — | Text shown when feature is off |
| `processing` | `{feature}` | When a tree chain starts (e.g. "tree breaks") |
| `processing-done` | `{feature}` | When a tree chain finishes |

**Example customization**

```yaml
prefix: "&2[TreeCap]&r "
processing: "&7Chopping {feature}… hang tight!"
processing-done: "&aDone chopping {feature}!"
toggle-self: "&7{feature} is now {state}&7."
```

---

## Player data

**Folder:** `plugins/NormalTreeCapitator/playerdata/`  
**File per player:** `<uuid>.yml`

```yaml
enabled: true
structure-protection: true
language: EN-us   # optional; omit or null = use server default from config.yml
```

| Key | Meaning |
|-----|---------|
| File missing | Uses `defaults.enabled` from `config.yml`; structure protection on; no personal language |
| `enabled: true` | Tree cap allowed (still needs permission + other checks) |
| `enabled: false` | Player turned it off with `/tc toggle` |
| `structure-protection` | Whether server structure protection applies to this player |
| `language` | Personal chat language code (e.g. `DE-de`); missing = server default |

Data is saved when a player toggles tree cap, structure protection, or personal language. Deleting a player's file resets them to defaults on next join.

---

## How it works

### Activation checks

When a player breaks a block, the plugin runs these checks **in order**:

1. Block is not already being broken by tree cap (anti-recursion).
2. Game mode is Survival or Adventure.
3. A configured **group** matches the broken block, held tool, and player **group permission** (gated groups preferred).
4. Player has `normaltreecapitator.use`.
5. Player has tree cap **enabled** (toggle / default).
6. Invincible replant protection (if breaking a protected sapling).
7. **Sneak gate** passes (`must-sneak`).
8. Not on **cooldown** (`cooldown-ticks`).
9. **Tool** is usable (axe durability / `break-tool` when `need-tool` is true).
10. Flood fill finds at least one block after **durability budget** trim (per-player damage rules apply).
11. **Structure protection** passes (chain looks like a natural tree) when `structure-protection` is true.

If all pass, the original break is cancelled and the chain runs.

---

### Sneak behavior

```
must-sneak: true  →  sneak to cap, stand for vanilla
must-sneak: false →  stand to cap, sneak for vanilla
```

Shift is tracked from `PlayerToggleSneakEvent` plus `player.isSneaking()` at break time.

---

### Flood fill & connectivity

1. Start at the broken block.
2. Collect all blocks in the same **group** within `search-radius` of any already-collected block.
3. Stop at `max-chain` (or unlimited if `-1`).
4. **Trunks first** — logs, wood, stems, and hyphae are ordered before leaves so durability and limits apply sensibly.
5. On Folia, only **loaded chunks owned by the current region** are read (no sync loads).

Species does **not** affect connectivity — only whether blocks share the same group list. Species **does** affect replant sapling matching.

---

### Sync vs async breaking

| | Sync | Async (waves) |
|---|------|----------------|
| **When** | Chain size ≤ `async-start` | Chain size > `async-start` |
| **Speed** | All blocks scheduled immediately | `blocks-per-tick` per wave, `async-delay` between waves |
| **Messages** | Processing + done chat messages | Same |
| **Drops** | Merged or per-block per `merge-item-drops` | Same |
| **Replant** | After last block + 1 tick settle | Same |

---

### Drops

- Drops come from vanilla `block.getDrops(tool, player)`.
- **`merge-item-drops: true`** — one pile at origin after chain completes.
- **`merge-item-drops: false`** — items spawn as each block breaks.
- Replant sapling consumption pulls from the accumulated drop pool when `replant-consume-saplings: true`.

---

### Tool durability

- Applied **per block** as it breaks (not all upfront).
- Cost per block from [block-damages](#block-damages) (default 1 for logs, 0 for leaves).
- **Unbreaking** enchantment rolled per damage point.
- If `break-tool: false`, costly blocks (logs) are capped so the axe keeps 1 durability. Connected **0-cost** foliage matching those logs is broken only when **every trunk** in the chain fits the budget — a partial cut leaves the canopy on the remaining logs (no free saplings are spawned).

---

### Replant system

When `replant: true`:

1. **During break** — each log records position, species, ground validity, and expected sapling.
2. **After all blocks break** — lowest log per column becomes a stump candidate.
3. **2×2 expansion** — dark oak / multi-column bases add sibling corners when those columns were broken.
4. **One tick later** — saplings planted at each stump on that block's region thread.
5. **Consumption** — if `replant-consume-saplings: true`, one matching sapling removed from **real** tree drops per plant. No sapling in drops → that stump is skipped.

With `break-tool: false`, connected foliage that costs **0** durability breaks only when the durability budget covers **every trunk** in the chain (matching tree families), so a short axe never strips leaves off leftover logs. Foliage that costs durability is not broken past the budget. Partial trunk cuts leave the canopy; finish the trunks (or repair the axe) to take leaves and feed replant.

Supported replant types include overworld saplings, mangrove propagules, crimson/warped fungus, and pale oak where the server version supports it.

Replanted blocks use direct `setType` (no synthetic place event) to avoid false cancellations while players are sneaking.

---

### Protection plugins

Each block in the chain fires a synthetic **`BlockBreakEvent`** with drops and XP disabled. If a protection plugin **cancels** the event for that block, that block is **skipped** — the rest of the tree still breaks.

This applies to WorldGuard, GriefPrevention, Lands, Residence, CoreProtect logging, and similar plugins that listen to break events.

---

### Scheduling (Folia)

| Work type | Scheduler |
|-----------|-----------|
| Player cooldown / chat messages | Entity scheduler (player) |
| Breaking each block | Region scheduler (block location) |
| Replant at stump | Region scheduler (stump location) |

This keeps all block access on the owning region thread.

---

## Default content

**Trees group includes (where supported by your MC version):**

- Overworld logs, stripped logs, wood, stripped wood
- Cherry, mangrove, pale oak variants
- Nether crimson/warped stems and hyphae (incl. stripped)
- All major leaf types, azalea leaves
- Nether wart blocks, shroomlight

**Other group includes:**

- Mushroom stem, brown mushroom block, red mushroom block

**Tools (both default groups):**

- Wooden through netherite axe

---

## Building from source

**Prerequisites:** Java 17+, Maven 3.8+

```bash
cd NormalTreeCapitator
mvn clean package
```

**Output:**

```
target/NormalTreeCapitator-1.0.5.jar
```

The JAR is shaded (bStats relocated). No extra libraries needed at runtime.

The built plugin version string appears in `/plugins` as e.g. `1.0.5 Build 3` (build number auto-increments each package).

---

## Troubleshooting

### Tree capitator does nothing

- Confirm **`normaltreecapitator.use`** permission.
- Run **`/tc toggle`** — player may have disabled it.
- Check **`must-sneak`** — wrong sneak state = vanilla break only.
- Confirm the block is in a group's **`blocks`** list.
- Confirm the held item is in that group's **`tools`** list (if `need-tool: true`).
- Check **`cooldown-ticks`** — player may still be on cooldown.
- Enable **`debug: true`**, reload, break a log, and read `[TreeCap] evaluate` / `blocked:` lines in `latest.log`.

### Only one block breaks (vanilla)

- Sneak gate blocked tree cap (see [Sneak behavior](#sneak-behavior)).
- Toggle is off or permission missing.
- Block not in any group.

### Chain stops early / axe stops mid-tree

- **`max-chain`** too low.
- **`break-tool: false`** with low durability — chain capped to preserve 1 durability.
- Protected blocks in the middle — skipped by claim plugins.

### Replant not working

- **`replant: false`** in config.
- **`replant-consume-saplings: true`** but no saplings in drops — need the full trunk budget so 0-cost leaves break and drop saplings; a partial durability cut leaves the canopy on remaining logs.
- Ground under stump not valid (mid-tree logs don't replant).
- Enable **`debug: true`** and search for `REPLANT` / `no-sapling-in-drops` lines.

### Unknown block warnings on startup

- Block ID does not exist on your Minecraft version (e.g. `pale_oak_log` on early 1.20). Remove from config or upgrade; other blocks still work.

### Config changes not applying

- Run **`/tc reload`** or restart.
- Edit **`plugins/NormalTreeCapitator/config.yml`**, not the JAR copy.
- `groups` and `block-damages` are never overwritten by jar defaults — if a section is missing, add it yourself (it will not regenerate).

### Lag on huge trees

- Lower **`blocks-per-tick`** (e.g. `25`–`50`).
- Lower **`async-start`** so wave breaking starts sooner.
- Increase **`async-delay`** slightly to spread waves.
- Lower **`max-chain`** to cap tree size.

### Debug log cheat sheet

| Log fragment | Meaning |
|--------------|---------|
| `evaluate sneaking=` | Sneak state at break time |
| `blocked: must-sneak` | Sneak gate failed |
| `blocked: structure-protection` | Chain looks like a build/structure — vanilla break only |
| `blocked: on cooldown` | Wait for cooldown |
| `skip: tool durability budget exhausted` | Axe can't afford full chain |
| `START ... mode=async/sync` | Chain accepted |
| `RESULT=SKIP reason=protected` | Claim plugin blocked that block |
| `REPLANT ... RESULT=PLANTED` | Stump replanted successfully |

---

## Metrics

This plugin includes [bStats](https://bstats.org/) for **anonymous** usage statistics (plugin ID: **32277**). No player names, UUIDs, or world data are sent through bStats.

---

## Changelog

| Version | Notes |
|---------|--------|
| **1.0.5** (current) | Natural-tree structure protection; 30-minute Pastebin poll, join checks, 3-hour reminders — see **[1.0.5.md](1.0.5.md)** |
| **1.0.4** | `/tc status`, `user` / `admin.*` packs, optional group & damage `permission:` gates — see **[1.0.4.md](1.0.4.md)** |
| **1.0.3** | Tool-aware groups, config ownership, durability/replant leaf handling, `/normaltreecap version`, Pastebin updates — see **[1.0.3.md](1.0.3.md)** |
| **1.0.2** | Prior stable release |

---

## Links

| | |
|--|--|
| **Download (Modrinth)** | https://modrinth.com/plugin/normal-tree-capitator/versions |
| **GitHub** | https://github.com/agentsix1/NormalTreeCapitator |
| **Version feed** | https://pastebin.com/raw/nc6CbGem |
| **Discord** | https://discord.normalsurvival.com |
| **Issues** | https://github.com/agentsix1/NormalTreeCapitator/issues |

---

*Questions or feedback? Join [Normal Survival on Discord](https://discord.normalsurvival.com).*
