# Kestrel (Modrinth / CurseForge page text)

Copy the body below into the project description. Replace the screenshot placeholders before publishing.

---

**Kestrel** helps you shoot. Hold a bow, crossbow or trident and see where the projectile will fly, which targets
you can hit, and where a moving target will be when the projectile arrives. An optional second mode turns the
camera to that point for you.

Client-side. No server install needed.

## Features
- Trajectory line and impact point for bow, crossbow (Multishot, firework rockets) and trident
- Target boxes that turn green when your shot would hit
- Lead prediction that accounts for your ping
- **Mode A**: you aim, Kestrel shows. **Mode B**: the camera turns to the aim point by itself (press R to switch)
- Mode B aims for the head when the target is on the ground, and for the middle of the hitbox when it jumps or is covered
- Pick what can be targeted: players, hostile mobs, animals, villagers, others. Animals and villagers are off by default
- Themes, colours, shapes and sounds you can change in game (Cloth Config + Mod Menu, optional)

## Fair play
Mode B is aim assistance and many servers do not allow it. Kestrel reminds you the first time you use it on a server,
you can turn it off in the settings, and **servers can switch any feature off** (see below).
You are responsible for following the rules of the server you play on.

## For server owners
Create a scoreboard team with a reserved name and the matching feature turns off for every player using Kestrel.
Works on vanilla, Paper, Fabric, anything that runs datapacks. The download list has a ready datapack
(`kestrel_rules`): put it in `<world>/datapacks`, `/reload`, then for example `/function kestrel_rules:modeb/off`.
Rules: Mode B, prediction/ghost, target boxes, trajectory line, targeting players, or everything.
This is a request the mod honours, not anti-cheat: keep your normal anti-cheat.

## Requirements
Fabric Loader, Fabric API. Optional: Cloth Config, Mod Menu. Minecraft 1.21.11 (Java 21), 26.2 and 26.3 (Java 25).

## Links
Source and issues: https://github.com/phudit-gm/kestrel  -  License: MIT
