# Kestrel

A client-side Fabric mod that helps you shoot. Hold a bow, crossbow or trident and Kestrel shows where the
projectile will fly, which targets are in range, and where a moving target will be when the projectile arrives.
An optional second mode (Mode B) turns the camera to that point for you.

No server install is needed. Servers can switch any feature off (see [For server owners](#for-server-owners)).

| Minecraft | Java | Notes |
|-----------|------|-------|
| 1.21.11   | 21   | |
| 26.2      | 25   | |
| 26.3      | 25   | |

Needs [Fabric API](https://modrinth.com/mod/fabric-api). [Cloth Config](https://modrinth.com/mod/cloth-config) and
[Mod Menu](https://modrinth.com/mod/modmenu) are optional and add the in-game settings screen.

## What it does

- **Trajectory and impact point** for bow, crossbow (including Multishot and firework rockets) and trident.
- **Target boxes** around what you can hit. The box turns green when the projectile's path touches the target.
- **Lead prediction (the "ghost")** shows where a moving target will be when the arrow arrives, including your ping.
- **Mode A (preview)**: you aim, Kestrel shows. **Mode B (auto aim)**: the camera turns to the aim point by itself
  and the mouse switches target instead. Press **R** to switch between A and B.
- Sounds, colours, shapes and themes you can change in the settings screen or in `config/kestrel/theme.json`
  (`/kestrel reload`). Themes: `pixel_gradient` (default) and `classic`.

## Mode B and fair play

Mode B is aim assistance. Many servers, especially PvP and competitive ones, do not allow it. Kestrel shows a
reminder in chat the first time you use it on a server. **You are responsible for following the rules of the server
you play on.** You can turn Mode B off completely in Settings > Mode B, and servers can switch it off for you.

Mode B aims for the head while the target stands on the ground and the head is not behind a block, and for the middle
of the hitbox when the target is jumping, falling, swimming or covered. This can be turned off.

## Who it targets

By default: hostile mobs and players in Mode A, hostile mobs only in Mode B. Animals, villagers and other creatures
are off. In Settings > Targeting and Mode B you can choose the categories separately for each mode, and add
entity ids that are always or never targeted (`minecraft:creeper`, or `somemod:*` for every creature of a mod).
Tamed animals are never targeted unless you change it.

## For server owners

Kestrel reads scoreboard teams with reserved names. Create one and the matching feature turns off for every
player who uses Kestrel. This works on vanilla, Paper, Fabric and any server that runs datapacks; you do not
install anything on the server.

The easiest way is the datapack in [`datapacks/kestrel_rules`](datapacks/kestrel_rules). Copy it to
`<world>/datapacks`, run `/reload`, then for example:

```
/function kestrel_rules:modeb/off            no Mode B (auto aim)
/function kestrel_rules:predict/off          no lead prediction / ghost (also no Mode B)
/function kestrel_rules:lock/off             no target boxes or locking (also no prediction, no Mode B)
/function kestrel_rules:path/off             no trajectory line or impact point
/function kestrel_rules:players/off          players cannot be targeted
/function kestrel_rules:all/off              Kestrel does nothing
/function kestrel_rules:preset/pvp_safe      no ghost, no Mode B, no targeting players
```

Every rule also has `/on`. Without the datapack you can run the same thing by hand, for example
`/team add ks_off_modeb`. Team names: `ks_off_all`, `ks_off_path`, `ks_off_lock`, `ks_off_predict`, `ks_off_modeb`,
`ks_off_players`.

**This is a request that the mod honours, not anti-cheat.** Someone who modifies the mod or uses another client is
not affected. Keep your usual anti-cheat.

## Installing

1. Install Fabric Loader and Fabric API for your Minecraft version.
2. Put `kestrel-<version>.jar` in the `mods` folder.
3. Optional: Cloth Config and Mod Menu for the settings screen.
4. In game, hold a bow and draw it. **R** switches Mode A / B (rebind in Controls > Kestrel).

## Settings files

- `config/kestrel/settings.json`: who can be targeted, Mode B tuning.
- `config/kestrel/theme.json`: look and sound.
- `config/kestrel.properties`: the last chosen mode.

## Building

Needs JDK 25 to run Gradle. Inside a version folder: `./gradlew build`. The jar is in `build/libs`.
`shared/` holds the source used by all versions.

## License

MIT, see [LICENSE](LICENSE).
