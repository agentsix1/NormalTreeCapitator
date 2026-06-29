# NormalTreeCapitator

**Chop one log. Break the whole tree.**

NormalTreeCapitator lets players cut down entire trees — logs, leaves, nether wood, mushrooms, and more — in one go. Hold an axe, break a block, and everything connected comes down with it.

Built for survival servers that want something simple for players and flexible for admins.

**By Agentsix1 & Cristichi** · [GitHub](https://github.com/agentsix1/NormalTreeCapitator) · [Discord](https://discord.normalsurvival.com)

---

## What makes it different?

- **Works on Folia** — not just Paper. Also runs on Spigot and Bukkit.
- **Won't melt your server** — big trees break in small waves instead of all at once.
- **You pick the blocks** — add trees, mushrooms, bamboo, or anything else in config. No code edits.
- **Players can turn it off** — `/tc toggle` saves their choice.
- **Fully editable** — change settings and chat messages in YAML, then `/tc reload`.

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
| `/tc help` | See commands |

> Default config may require **sneaking** while breaking. Your server admin can change that.

---

## For server owners

**Commands** — `/tc` (also `/treecap`, `/treecapitator`)

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
| `config.yml` | Block lists, axes, limits, replant, sneak mode |
| `messages.yml` | Chat text and colors |

**Groups** — the fun part. You can make separate rules for trees, mushrooms, or custom blocks. Each group has its own block list and tool list. Trees won't chain into mushrooms unless you put them in the same group.

---

## Included out of the box

- All overworld log & leaf types (incl. cherry, mangrove, pale oak where supported)
- Nether stems, hyphae, wart blocks, shroomlight
- Huge mushrooms (separate group)
- Optional replant & sapling protection
- Axe durability, Unbreaking, unbreakable tools

---

## Links

- **Full guide:** https://github.com/agentsix1/NormalTreeCapitator
- **Discord:** https://discord.normalsurvival.com
- **Issues & source:** https://github.com/agentsix1/NormalTreeCapitator

---

*Uses [bStats](https://bstats.org/) for anonymous plugin stats. No player data is collected.*
