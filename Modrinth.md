# NormalTreeCapitator

**Chop one log. The whole tree comes down.**

Tired of punching the same oak for five minutes? NormalTreeCapitator is a Minecraft **tree capitator** / **tree feller** (yeah — timber vibes) for survival servers. Grab an axe, break one log, and the rest of the tree follows: logs, leaves, nether wood, huge mushrooms — whatever you set up.

And here’s the best part: **all the drops land where you first chopped.** No scavenger hunt across the canopy. Walk up to the stump, scoop the loot, move on.

Works on **Paper, Folia, Spigot, and Bukkit.** Easy for players. Plenty for admins to tweak.

<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/VrnVOFMA1xQ" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

**By Agentsix1** · [GitHub](https://github.com/agentsix1/NormalTreeCapitator) · [Modrinth](https://modrinth.com/plugin/normal-tree-capitator/versions) · [Discord](https://discord.normalsurvival.com)  
*Originally based on Cristichi’s [Tree Capitator](https://www.curseforge.com/minecraft/bukkit-plugins/cristichis-tree-capitator)*

---

## Why people pick it

- **One swing, whole tree** — classic tree capitator feel without the busywork
- **Drops at the stump** — everything piles where you started chopping, so pickup is easy
- **Folia-friendly** — runs on Folia, not only Paper/Spigot/Bukkit
- **Won’t eat your house** — structure protection keeps builds and villages safer; real trees still fall like they should
- **Big trees, chill server** — huge ones break in waves so TPS doesn’t throw a tantrum
- **Your trees, your rules** — mix oak with VIP axes, mushrooms, bamboo… all in YAML
- **Optional replant** — saplings/fungus can grow back after the tree’s done
- **Fair on axes** — leaves can cost zero durability; turn `break-tool` off and your axe won’t snap mid-tree
- **Plays nice with claims** — WorldGuard, GriefPrevention, Lands, CoreProtect, and friends still get a say per block
- **Languages** — bundled `EN-us`, `EN-gb`, `EN-sg`, `DE-de`, `ES-es`, `PT-br` (or drop in your own file)
- **You’re in control** — `/tc toggle` if you’re not feeling it; staff can peek with `/tc <player>`; `/tc status` shows progress while a giant is falling

> Full nerdy docs: **[GitHub README](https://github.com/agentsix1/NormalTreeCapitator)** · What’s new: **[1.0.5](https://github.com/agentsix1/NormalTreeCapitator/blob/main/1.0.5.md)**

---

## Works with

| | |
|---|---|
| **Minecraft** | 1.20+ |
| **Servers** | Paper · Folia · Spigot · Bukkit |
| **Java** | 17+ |

---

## Install

1. Grab it from [Modrinth](https://modrinth.com/plugin/normal-tree-capitator/versions)
2. Drop the jar in `plugins/`
3. Restart the server
4. Poke at `plugins/NormalTreeCapitator/config.yml` if you want
5. `/tc reload` after YAML edits (restart when you swap jars)

Done. Go hit a tree.

---

## For players

| Do this | What happens |
|--------|----------------|
| Break a log with an axe | Connected tree comes down (natural trees by default) |
| Watch the ground | Loot shows up at the block you first broke |
| `/tc` | Check if tree cap is on for you |
| `/tc toggle` | Flip it on/off for yourself |
| `/tc status` | See how far a big break has gotten (+ rough wait time) |
| `/tc help` | Commands you actually have access to |
| `/tc language <code>` | Set your own chat language *(only if the server grants it)* |

> Many servers make you **sneak** to tree-cap (`must-sneak: true`). Admins can flip that so standing tree-caps and sneak is a normal single break.

Tall trees might say **“Processing…”** for a sec. That’s normal — it’s clearing the canopy without lagging everyone.

---

## For server owners

**Commands:** `/tc` · aliases `/treecap`, `/treecapitator`, `/normaltreecap`

### Default player pack (`normaltreecapitator.user`)

Each one has its own permission. Give the whole pack, or pick and choose.

| Command / feature | Permission | Default |
|-------------------|------------|---------|
| Actually tree-capping | `normaltreecapitator.use` | `true` |
| `/tc` | `normaltreecapitator.status` | `true` |
| `/tc status` | `normaltreecapitator.progress` | `true` |
| `/tc help` | `normaltreecapitator.help` | `true` |
| `/tc toggle` | `normaltreecapitator.toggle` | `true` |

### Staff / optional

| Command | Permission | Default |
|---------|------------|---------|
| `/tc <player>` | `normaltreecapitator.admin.state` | op |
| `/tc toggle <player>` | `normaltreecapitator.admin.toggle.others` | op |
| `/tc reload` | `normaltreecapitator.admin.reload` | op |
| `/tc version` | `normaltreecapitator.version` | op |
| `/tc language server <code>` | `normaltreecapitator.admin.language` | op |
| `/tc language <code>` | `normaltreecapitator.language` | `false` (not in `user`) |
| `/tc structure-protection` | `normaltreecapitator.structure-protection` | `false` (not in `user`) |

**Packs:** `normaltreecapitator.user` (everyone by default) · `normaltreecapitator.admin` · `normaltreecapitator.*` (includes optional `language` + `structure-protection`)  
**Extra gates from config:** `normaltreecapitator.group.<name>` · `normaltreecapitator.damage.<name>`

| File | Job |
|------|-----|
| `config.yml` | Trees, axes, limits, server language, replant, sneak, drops, structure protection, async, VIP stuff |
| `languages/*.yml` | Chat text & colors (server default + optional personal override) |
| `playerdata/<uuid>.yml` | Per-player toggle, structure opt-out, personal language |

**Groups** — trees vs mushrooms vs custom junk, each with their own blocks/tools (and optional permission). Same log can sit in more than one group for tool tiers.

**Handy settings**

| Setting | Vibe |
|---------|------|
| `language` | Server default — `EN-us`, `EN-gb`, `EN-sg`, `DE-de`, `ES-es`, `PT-br` (or your own file in `languages/`) |
| `merge-item-drops` | **On by default** — all loot piles at the first chop spot |
| `structure-protection` | Real trees only; skip house/village wood |
| `structure-cleanup` | Still clear leftover leaf-only / lonely log stacks |
| `must-sneak` | Sneak to cap, or stand to cap |
| `replant` / `replant-consume-saplings` | Grow something back; optionally spend real sapling drops |
| `break-tool` / `block-damages` | How hard axes take a beating |
| `async-start` / `blocks-per-tick` / `async-delay` | How chunky big trees feel |
| `debug` | Loud `[TreeCap]` console logs when something’s weird |

---

## What’s new in 1.0.5

- Structure protection + cleanup for leftover leaves/logs
- `/tc`, `/tc <player>`, live `/tc status`, smarter `/tc help`
- Separate permission per player command
- `/tc version` is admin-only now
- Languages folder with `EN-us`, `EN-gb`, `EN-sg`, `DE-de`, `ES-es`, `PT-br`
- Server default via `language:` or `/tc language server <code>`
- Optional personal `/tc language <code>` (grant `normaltreecapitator.language` — not in `user`)
- `admin.state` (renamed from `admin.status`)
- Update checks that don’t spam every second
- Low-durability axes no longer strip leaves off leftover trunks

---

## Comes with

- Overworld logs & leaves (cherry, mangrove, pale oak when the server has them)
- Nether stems, hyphae, wart blocks, shroomlight
- Huge mushrooms (their own group)
- Replant + optional sapling protection
- Unbreaking / unbreakable tools handled
- Trunks eat durability before free leaves
- Staff update link when a new build drops

---

## Links

- **Download:** https://modrinth.com/plugin/normal-tree-capitator/versions
- **Guide:** https://github.com/agentsix1/NormalTreeCapitator
- **Discord:** https://discord.normalsurvival.com
- **Bugs & source:** https://github.com/agentsix1/NormalTreeCapitator/issues

---

*Uses [bStats](https://bstats.org/plugin/bukkit/NormalTreeCapitator/32277) for anonymous stats. No player data.*
