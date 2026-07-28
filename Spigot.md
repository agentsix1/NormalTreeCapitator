[B][SIZE=4]Chop one log. The whole tree comes down.[/SIZE][/B]

Tired of punching the same oak for five minutes? NormalTreeCapitator is a Minecraft [B]tree capitator[/B] / [B]tree feller[/B] (yeah — timber vibes) for survival servers. Grab an axe, break one log, and the rest of the tree follows: logs, leaves, nether wood, huge mushrooms — whatever you set up.

And here’s the best part: [B]all the drops land where you first chopped.[/B] No scavenger hunt across the canopy. Walk up to the stump, scoop the loot, move on.

Works on [B]Paper, Folia, Spigot, and Bukkit.[/B] Easy for players. Plenty for admins to tweak.

[MEDIA=youtube]VrnVOFMA1xQ[/MEDIA]

[B]By Agentsix1[/B] · [URL='https://github.com/agentsix1/NormalTreeCapitator']GitHub[/URL] · [URL='https://modrinth.com/plugin/normal-tree-capitator/versions']Modrinth[/URL] · [URL='https://discord.normalsurvival.com']Discord[/URL]
[I]Originally based on Cristichi’s [URL='https://www.curseforge.com/minecraft/bukkit-plugins/cristichis-tree-capitator']Tree Capitator[/URL][/I]

[SIZE=5][B]Why people pick it[/B][/SIZE]
[LIST]
[*][B]One swing, whole tree[/B] — classic tree capitator feel without the busywork
[*][B]Drops at the stump[/B] — everything piles where you started chopping, so pickup is easy
[*][B]Folia-friendly[/B] — runs on Folia, not only Paper/Spigot/Bukkit
[*][B]Won’t eat your house[/B] — structure protection keeps builds and villages safer; real trees still fall like they should
[*][B]Big trees, chill server[/B] — huge ones break in waves so TPS doesn’t throw a tantrum
[*][B]Your trees, your rules[/B] — mix oak with VIP axes, mushrooms, bamboo… all in YAML
[*][B]Optional replant[/B] — saplings/fungus can grow back after the tree’s done
[*][B]Fair on axes[/B] — leaves can cost zero durability; turn [I]break-tool[/I] off and your axe won’t snap mid-tree
[*][B]Plays nice with claims[/B] — WorldGuard, GriefPrevention, Lands, CoreProtect, and friends still get a say per block
[*][B]Languages[/B] — bundled [I]EN-us[/I], [I]EN-gb[/I], [I]EN-sg[/I], [I]DE-de[/I], [I]ES-es[/I], [I]PT-br[/I] (or drop in your own file)
[*][B]You’re in control[/B] — [I]/tc toggle[/I] if you’re not feeling it; staff can peek with [I]/tc <player>[/I]; [I]/tc status[/I] shows progress while a giant is falling
[/LIST]

[QUOTE]Full nerdy docs: [URL='https://github.com/agentsix1/NormalTreeCapitator'][B]GitHub README[/B][/URL] · What’s new: [URL='https://github.com/agentsix1/NormalTreeCapitator/blob/main/1.0.5.md']1.0.5[/URL][/QUOTE]

[SIZE=5][B]Works with[/B][/SIZE]
[TABLE]
[TR]
[TD][B]Minecraft[/B][/TD]
[TD]1.20+[/TD]
[/TR]
[TR]
[TD][B]Servers[/B][/TD]
[TD]Paper · Folia · Spigot · Bukkit[/TD]
[/TR]
[TR]
[TD][B]Java[/B][/TD]
[TD]17+[/TD]
[/TR]
[/TABLE]

[SIZE=5][B]Install[/B][/SIZE]
[LIST]
[*]Grab it from [URL='https://modrinth.com/plugin/normal-tree-capitator/versions']Modrinth[/URL]
[*]Drop the jar in [I]plugins/[/I]
[*]Restart the server
[*]Poke at [I]plugins/NormalTreeCapitator/config.yml[/I] if you want
[*][I]/tc reload[/I] after YAML edits (restart when you swap jars)
[/LIST]
Done. Go hit a tree.

[SIZE=5][B]For players[/B][/SIZE]
[TABLE]
[TR]
[TD][B]Do this[/B][/TD]
[TD][B]What happens[/B][/TD]
[/TR]
[TR]
[TD]Break a log with an axe[/TD]
[TD]Connected tree comes down (natural trees by default)[/TD]
[/TR]
[TR]
[TD]Watch the ground[/TD]
[TD]Loot shows up at the block you first broke[/TD]
[/TR]
[TR]
[TD][I]/tc[/I][/TD]
[TD]Check if tree cap is on for you[/TD]
[/TR]
[TR]
[TD][I]/tc toggle[/I][/TD]
[TD]Flip it on/off for yourself[/TD]
[/TR]
[TR]
[TD][I]/tc status[/I][/TD]
[TD]See how far a big break has gotten (+ rough wait time)[/TD]
[/TR]
[TR]
[TD][I]/tc help[/I][/TD]
[TD]Commands you actually have access to[/TD]
[/TR]
[TR]
[TD][I]/tc language <code>[/I][/TD]
[TD]Set your own chat language [I](only if the server grants it)[/I][/TD]
[/TR]
[/TABLE]

[QUOTE]Many servers make you [B]sneak[/B] to tree-cap ([I]must-sneak: true[/I]). Admins can flip that so standing tree-caps and sneak is a normal single break.[/QUOTE]

Tall trees might say [B]“Processing…”[/B] for a sec. That’s normal — it’s clearing the canopy without lagging everyone.

[SIZE=5][B]For server owners[/B][/SIZE]

[B]Commands:[/B] [I]/tc[/I] · aliases [I]/treecap[/I], [I]/treecapitator[/I], [I]/normaltreecap[/I]

[B]Default player pack ([I]normaltreecapitator.user[/I])[/B]
Each one has its own permission. Give the whole pack, or pick and choose.
[TABLE]
[TR]
[TD][B]Command / feature[/B][/TD]
[TD][B]Permission[/B][/TD]
[TD][B]Default[/B][/TD]
[/TR]
[TR]
[TD]Actually tree-capping[/TD]
[TD][I]normaltreecapitator.use[/I][/TD]
[TD][I]true[/I][/TD]
[/TR]
[TR]
[TD][I]/tc[/I][/TD]
[TD][I]normaltreecapitator.status[/I][/TD]
[TD][I]true[/I][/TD]
[/TR]
[TR]
[TD][I]/tc status[/I][/TD]
[TD][I]normaltreecapitator.progress[/I][/TD]
[TD][I]true[/I][/TD]
[/TR]
[TR]
[TD][I]/tc help[/I][/TD]
[TD][I]normaltreecapitator.help[/I][/TD]
[TD][I]true[/I][/TD]
[/TR]
[TR]
[TD][I]/tc toggle[/I][/TD]
[TD][I]normaltreecapitator.toggle[/I][/TD]
[TD][I]true[/I][/TD]
[/TR]
[/TABLE]

[B]Staff / optional[/B]
[TABLE]
[TR]
[TD][B]Command[/B][/TD]
[TD][B]Permission[/B][/TD]
[TD][B]Default[/B][/TD]
[/TR]
[TR]
[TD][I]/tc <player>[/I][/TD]
[TD][I]normaltreecapitator.admin.state[/I][/TD]
[TD]op[/TD]
[/TR]
[TR]
[TD][I]/tc toggle <player>[/I][/TD]
[TD][I]normaltreecapitator.admin.toggle.others[/I][/TD]
[TD]op[/TD]
[/TR]
[TR]
[TD][I]/tc reload[/I][/TD]
[TD][I]normaltreecapitator.admin.reload[/I][/TD]
[TD]op[/TD]
[/TR]
[TR]
[TD][I]/tc version[/I][/TD]
[TD][I]normaltreecapitator.version[/I][/TD]
[TD]op[/TD]
[/TR]
[TR]
[TD][I]/tc language server <code>[/I][/TD]
[TD][I]normaltreecapitator.admin.language[/I][/TD]
[TD]op[/TD]
[/TR]
[TR]
[TD][I]/tc language <code>[/I][/TD]
[TD][I]normaltreecapitator.language[/I][/TD]
[TD][I]false[/I] (not in [I]user[/I])[/TD]
[/TR]
[TR]
[TD][I]/tc structure-protection[/I][/TD]
[TD][I]normaltreecapitator.structure-protection[/I][/TD]
[TD][I]false[/I] (not in [I]user[/I])[/TD]
[/TR]
[/TABLE]

[B]Packs:[/B] [I]normaltreecapitator.user[/I] (everyone by default) · [I]normaltreecapitator.admin[/I] · [I]normaltreecapitator.*[/I] (includes optional [I]language[/I] + [I]structure-protection[/I])
[B]Extra gates from config:[/B] [I]normaltreecapitator.group.<name>[/I] · [I]normaltreecapitator.damage.<name>[/I]

[TABLE]
[TR]
[TD][B]File[/B][/TD]
[TD][B]Job[/B][/TD]
[/TR]
[TR]
[TD][I]config.yml[/I][/TD]
[TD]Trees, axes, limits, server language, replant, sneak, drops, structure protection, async, VIP stuff[/TD]
[/TR]
[TR]
[TD][I]languages/*.yml[/I][/TD]
[TD]Chat text & colors (server default + optional personal override)[/TD]
[/TR]
[TR]
[TD][I]playerdata/<uuid>.yml[/I][/TD]
[TD]Per-player toggle, structure opt-out, personal language[/TD]
[/TR]
[/TABLE]

[B]Groups[/B] — trees vs mushrooms vs custom junk, each with their own blocks/tools (and optional permission). Same log can sit in more than one group for tool tiers.

[B]Handy settings[/B]
[TABLE]
[TR]
[TD][B]Setting[/B][/TD]
[TD][B]Vibe[/B][/TD]
[/TR]
[TR]
[TD][I]language[/I][/TD]
[TD]Server default — [I]EN-us[/I], [I]EN-gb[/I], [I]EN-sg[/I], [I]DE-de[/I], [I]ES-es[/I], [I]PT-br[/I] (or your own file in [I]languages/[/I])[/TD]
[/TR]
[TR]
[TD][I]merge-item-drops[/I][/TD]
[TD][B]On by default[/B] — all loot piles at the first chop spot[/TD]
[/TR]
[TR]
[TD][I]structure-protection[/I][/TD]
[TD]Real trees only; skip house/village wood[/TD]
[/TR]
[TR]
[TD][I]structure-cleanup[/I][/TD]
[TD]Still clear leftover leaf-only / lonely log stacks[/TD]
[/TR]
[TR]
[TD][I]must-sneak[/I][/TD]
[TD]Sneak to cap, or stand to cap[/TD]
[/TR]
[TR]
[TD][I]replant[/I] / [I]replant-consume-saplings[/I][/TD]
[TD]Grow something back; optionally spend real sapling drops[/TD]
[/TR]
[TR]
[TD][I]break-tool[/I] / [I]block-damages[/I][/TD]
[TD]How hard axes take a beating[/TD]
[/TR]
[TR]
[TD][I]async-start[/I] / [I]blocks-per-tick[/I] / [I]async-delay[/I][/TD]
[TD]How chunky big trees feel[/TD]
[/TR]
[TR]
[TD][I]debug[/I][/TD]
[TD]Loud [I][TreeCap][/I] console logs when something’s weird[/TD]
[/TR]
[/TABLE]

[SIZE=5][B]What’s new in 1.0.5[/B][/SIZE]
[LIST]
[*]Structure protection + cleanup for leftover leaves/logs
[*][I]/tc[/I], [I]/tc <player>[/I], live [I]/tc status[/I], smarter [I]/tc help[/I]
[*]Separate permission per player command
[*][I]/tc version[/I] is admin-only now
[*]Languages folder with [I]EN-us[/I], [I]EN-gb[/I], [I]EN-sg[/I], [I]DE-de[/I], [I]ES-es[/I], [I]PT-br[/I]
[*]Server default via [I]language:[/I] or [I]/tc language server <code>[/I]
[*]Optional personal [I]/tc language <code>[/I] (grant [I]normaltreecapitator.language[/I] — not in [I]user[/I])
[*][I]admin.state[/I] (renamed from [I]admin.status[/I])
[*]Update checks that don’t spam every second
[*]Low-durability axes no longer strip leaves off leftover trunks
[/LIST]

[SIZE=5][B]Comes with[/B][/SIZE]
[LIST]
[*]Overworld logs & leaves (cherry, mangrove, pale oak when the server has them)
[*]Nether stems, hyphae, wart blocks, shroomlight
[*]Huge mushrooms (their own group)
[*]Replant + optional sapling protection
[*]Unbreaking / unbreakable tools handled
[*]Trunks eat durability before free leaves
[*]Staff update link when a new build drops
[/LIST]

[SIZE=5][B]Links[/B][/SIZE]
[LIST]
[*][B]Download:[/B] [URL]https://modrinth.com/plugin/normal-tree-capitator/versions[/URL]
[*][B]Guide:[/B] [URL]https://github.com/agentsix1/NormalTreeCapitator[/URL]
[*][B]Discord:[/B] [URL]https://discord.normalsurvival.com[/URL]
[*][B]Bugs & source:[/B] [URL]https://github.com/agentsix1/NormalTreeCapitator/issues[/URL]
[/LIST]

[I]Uses [URL='https://bstats.org/plugin/bukkit/NormalTreeCapitator/32277']bStats[/URL] for anonymous stats. No player data.[/I]
