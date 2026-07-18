# NormalTreeCapitator

**Chop one log. Break the whole tree.**

NormalTreeCapitator lets players cut down entire trees — logs, leaves, nether wood, mushrooms, and more — in one go. Hold an axe, break a block, and everything connected comes down with it.

Built for survival servers that want something simple for players and flexible for admins.

**By Agentsix1** · [GitHub](https://github.com/agentsix1/NormalTreeCapitator) · [Discord](https://discord.normalsurvival.com)  
*Original Dev Cristichi [Cristichi's Tree Capitator](https://www.curseforge.com/minecraft/bukkit-plugins/cristichis-tree-capitator)*

---

## What makes it different?

- **Works on Folia** — not just Paper. Also runs on Spigot and Bukkit.
- **Won't melt your server** — big trees break in small waves instead of all at once.
- **You pick the blocks** — add trees, mushrooms, bamboo, or anything else in config. No code edits.
- **Players can turn it off** — `/tc toggle` saves their choice.
- **Fully editable** — change settings and chat messages in YAML, then `/tc reload`.
- **Claim-friendly** — each block in a chain fires a break check, so protection plugins (WorldGuard, GriefPrevention, Lands, CoreProtect, etc.) can skip protected blocks while the rest still break.
- **Smart replant** — optional auto-replant runs after the full tree is down, uses the correct sapling/fungus per log type, and supports 2×2 dark oak bases.
- **Fair on tools** — leaves can cost zero durability; when `break-tool` is off, chains stop before your axe breaks.

> Full docs, config tables, and troubleshooting: **[GitHub README](https://github.com/agentsix1/NormalTreeCapitator)**

---

## Works with

| | |
|---|---|
| **Minecraft** | 1.20+ |
| **Servers** | Paper · Folia · Spigot · Bukkit |
| **Java** | 17+ |

---

## Install

1. Drop the JAR in `plugins/`
2. Start the server
3. Tweak `plugins/NormalTreeCapitator/config.yml` if you want
4. `/tc reload` when you change something

That's it.

---

## For players

| Do this | What happens |
|--------|----------------|
| Break a log with an axe | The whole connected tree breaks |
| `/tc toggle` | Turn tree cap on or off for yourself |
| `/tc version` (or `/normaltreecap version`) | Show plugin version |
| `/tc help` | See commands |

> Default config may require **sneaking** while breaking. Your server admin can flip `must-sneak` so tree cap works while standing instead — sneaking then becomes a normal single-block break.

Large trees show a short **“Processing tree breaks…”** message while the chain finishes.

---

## For server owners/admins

**Commands** — `/tc` (also `/treecap`, `/treecapitator`, `/normaltreecap`)

| Command | Who needs permission |
|---------|---------------------|
| `/tc toggle` | Everyone (default) |
| `/tc toggle <player>` | Staff |
| `/tc reload` | Staff |

**Main permissions**

- `normaltreecapitator.use` — use tree capitator *(default: everyone)*
- `normaltreecapitator.toggle` — toggle for yourself
- `normaltreecapitator.reload` — reload configs

**Config files**

| File | What it's for |
|------|----------------|
| `config.yml` | Block lists, axes, limits, replant, sneak mode, durability costs, async tuning |
| `messages.yml` | Chat text and colors |

**Groups** — the fun part. You can make separate rules for trees, mushrooms, or custom blocks. Each group has its own block list and tool list. Trees won't chain into mushrooms unless you put them in the same group.

**Notable settings (1.0.3+)**

| Setting | What it does |
|---------|----------------|
| `must-sneak` | Sneak to cap (`true`) or cap while standing (`false`) |
| `replant` / `replant-consume-saplings` | Auto-replant stumps; optionally consume saplings from drops |
| `merge-item-drops` | One drop pile at the stump vs drops at each block |
| `break-tool` / `block-damages` | Control axe wear — leaves can cost 0 durability |
| `cooldown-ticks` | Delay between tree-cap uses |
| `async-start` / `blocks-per-tick` / `async-delay` | Wave size and timing for huge trees |
| `debug` | `[TreeCap]` lines in console for troubleshooting |

---

## Included out of the box

- All overworld log & leaf types (incl. cherry, mangrove, pale oak where supported)
- Nether stems, hyphae, wart blocks, shroomlight
- Huge mushrooms (separate group)
- Optional replant & sapling protection
- Axe durability, Unbreaking, unbreakable tools
- Trunk-first breaking so logs take durability before leaves

---

## Links

- **Full guide:** https://github.com/agentsix1/NormalTreeCapitator
- **Discord:** https://discord.normalsurvival.com
- **Issues & source:** https://github.com/agentsix1/NormalTreeCapitator/issues

---

*Uses [bStats](https://bstats.org/) for anonymous plugin stats. No player data is collected.*
