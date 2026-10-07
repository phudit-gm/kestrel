# Kestrel Step 1 — Project setup, projectile physics core, bow trajectory preview

## Objective
A buildable Minecraft Java mod named Kestrel (mod id `kestrel`) that, while the player draws a bow, shows an accurate predicted arrow trajectory, highlights lockable entities in range, and indicates full charge. Works on any server with no server-side install.

## Context
- Relevant files/modules: none yet — new project.
- Current state: no code exists. This is Step 1 of 5. Later steps add target locking, motion prediction, an auto-aim mode, server-side homing, and more weapons. Code structure must leave room for them.

## Scope
**In scope**
- New mod project for the loader and Minecraft version in Open questions.
- Package layout with four areas, even if some are empty for now:
  - `core` — shared physics and math (used by both client and server later)
  - `link` — client/server handshake (empty placeholder in this step)
  - `sight` — client-side visuals and aiming
  - `flight` — server-side arrow steering (empty placeholder in this step)
- `core` projectile simulator for arrows: given start position, start velocity, and world, step tick by tick using the game's own arrow gravity, air drag, and water drag for the chosen version. Read these values from the game's projectile code; do not guess them. Stops at the first block or entity hit.
- Bow draw-strength → launch speed, using the same formula the game uses.
- While the player is drawing a bow (main hand or offhand):
  - Render the predicted trajectory as a line and mark the impact point. Updates every frame as draw strength changes.
  - Draw a yellow box around every lockable entity within lock range.
    - Lock range = maximum distance an arrow at full charge can reach from the player's current position (computed, not a fixed number).
    - Cap the number of boxes (default 32, nearest first).
    - Default filter: include hostile mobs and players; exclude villagers, wandering traders, and tamed animals. Keep filter values in one settings class so Step 5 can expose them in a config screen.
  - If the predicted trajectory intersects an entity's box, that box turns green and shows the label `TARGET`.
  - Show a clear indicator when the bow is fully charged.
- Boxes are drawn slightly larger than the entity hitbox so they do not overlap or flicker with the vanilla debug hitbox view (F3+B) or other hitbox-display mods.
- Nothing is shown when not drawing a bow.

**Out of scope**
- Target locking, motion prediction, ghost models, aim modes.
- Any server-side behavior or networking.
- Crossbow, trident, and other weapons.
- Sounds, config screen.
- New items, blocks, or entities of any kind.

## Constraints
- Do not modify: vanilla items, entities, or registries. The mod must add no registry entries so players without the mod can still join servers that have it later.
- Conventions: loader's standard project template; all colors, ranges, and limits defined as named constants in one settings class; physics code in `core` must not depend on client-only classes.
- Dependencies: only the loader and its standard API.
- Write pseudocode for the projectile simulator as a comment block before implementing it.

## Acceptance criteria
- [ ] Project builds with the loader's standard build command and produces a mod jar.
- [ ] Trajectory and impact marker appear only while drawing a bow, and move as draw strength increases.
- [ ] On flat ground, at full charge and at least 3 different pitch angles (e.g. 0°, 20°, 45°), the real arrow lands within 1 block of the predicted impact marker.
- [ ] Shooting into water shows a shorter trajectory consistent with where the real arrow stops.
- [ ] Yellow boxes appear only on filtered entity types within lock range; villagers and tamed animals have no box by default.
- [ ] With 100+ mobs nearby, at most 32 boxes are drawn and the game shows no noticeable FPS drop compared with not drawing.
- [ ] A box turns green with `TARGET` exactly when the predicted line passes through that entity.
- [ ] Full-charge indicator appears at the moment the bow reaches full draw.
- [ ] With F3+B enabled, Kestrel boxes and the white vanilla hitboxes are both visible with no flickering.
- [ ] All of the above works when joined to a vanilla dedicated server with no mod installed on the server.

## Open questions
- Mod loader: Fabric, NeoForge, or Forge?
- Exact Minecraft version?
- Should "friends" be excluded from boxes by default? If yes, how is a friend defined (same team/scoreboard team, a manual list)?
