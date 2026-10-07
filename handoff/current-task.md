# Kestrel hand-off (2026-10-07)

## State
Step 1 of 5 is DONE and tested in game by the user (all acceptance items pass).
Repo: https://github.com/phudit-gm/kestrel (branch `main`, all work pushed).
Next: Step 2, brief at `brief/brief-kestrel-step2-lock-prediction-modeA.md`. Read it, then ask the open questions before coding.

## Layout
- `mc-1.21.11/`, `mc-26.3/`: separate Fabric Gradle projects (Fabric, MC 1.21.11 / Java 21 and MC 26.3 / Java 25).
- `shared/main/java`: `core` (ArrowPhysics, ProjectileSimulator, LockRange, TrajectoryResult), `KestrelSettings` (all colours/limits/filter), `link` and `flight` (empty placeholders for Step 4).
- `shared/client/java`: `sight` (SightRenderer, LockFilter). Both version projects add these as extra srcDirs in build.gradle.
- Per-version only: `KestrelClient.java` (event registration), `fabric.mod.json`.

## Decisions and facts
- Arrow physics read from game bytecode; identical in 1.21.11 and 26.3 (gravity 0.05, air drag 0.99, water drag 0.6, speed = power*3.0, spawn at eye-0.1). Order per tick: water drag first if in water, clip+entity hit, move, air drag if not in water, gravity.
- Lock range = max flight distance at full charge, computed once, ~143 blocks.
- Drawing uses vanilla Gizmos (same API in both versions). Events: `WorldRenderEvents.BEFORE_DEBUG_RENDER` (1.21.11), `LevelRenderEvents.BEFORE_GIZMOS` (26.3). The per-frame gizmo collector is active there.
- Trajectory line is drawn from the bow hand and slides onto the real path over 12 ticks (visual only; physics untouched). Without this the arc looked like a vertical line from behind.
- Boxes are depth-tested on purpose (no wallhack on PvP servers).
- TARGET label appears above the entity and again near the crosshair, under FULL.
- Default filter: hostile mobs + players; villagers, wandering traders, tamed animals excluded. "Friends" not implemented (user chose to skip until Step 5).
- No registry entries, so it works on vanilla servers.

## Build / deploy
`$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'; cd mc-<ver>; .\gradlew.bat build` (JDK 25 needed to run Gradle even for 1.21.11).
Deployed to Prism instances `PvP 1.21.11` and `Survival 26.3` (mods folder). The jar is locked while the game runs; close the game first.

## Instance notes
- ptp (Projectiles Trajectory Prediction) was removed from those two instances; jars saved in `C:\dev\minecraft\disabled-mods\`. It has no side view; its line only reads better because it starts at the hand.
- `Survival 26.3` crashed on launch because of `particlerain ...+26.2` (26.2 build on 26.3, not related to Kestrel); renamed to `.jar.disabled`.

## Not done / ideas
- Step 2-5 (lock, prediction, auto-aim, server flight, other weapons, config screen).
- Other Prism instances (PvP 26.3, Survival 1.21.11) do not have Kestrel yet.
