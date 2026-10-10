# Kestrel hand-off (updated 2026-10-11)

## State
Steps 1 to 3 and 5 are done and tested in game. Step 4 (server handshake / homing) was skipped on purpose.
Step 6 (release preparation, 2026-10-11) is **built and pushed but only partly tested in game**: see "Step 6" at the end.
The next job is: test the items in `release/CHECKLIST.md`, then publish (nothing has been published anywhere).
Repo: https://github.com/phudit-gm/kestrel (branch `main`).

## Layout
- `mc-1.21.11/`, `mc-26.2/`, `mc-26.3/`: separate Fabric Gradle projects (1.21.11 / Java 21; 26.2 and 26.3 / Java 25). Each adds `shared/main/java`, `shared/main/resources` and `shared/client/java` as source dirs in `build.gradle`.
- `shared/main/java`:
  - `core`: ArrowPhysics, ProjectileSimulator, LockRange, TrajectoryResult (Step 1 physics, unchanged).
  - `feedback`: `FeedbackEvent` (all events incl. later ones), `FeedbackBus` (level-triggered: emit every active event each frame, then `frameEnd()`), payload records, `theme/` (`Theme`, `ThemeLoader`, `ColorSpec`, `Easing`).
  - `KestrelSettings`: only behaviour constants (lock filter, simulation cap). Visual/audio values live in themes.
- `shared/main/resources/assets/kestrel/themes/`: `classic.json` (Step 1 look; lists every key; also the fallback for missing keys) and `pixel_gradient.json` (default, formerly `khaopun`; the old name is still accepted in theme.json).
- `shared/client/java`: `sight/SightController` (logic only, emits events), `sight/LockFilter`, `feedback/FeedbackLayer` (draws/plays from the theme, per-element animated tracks), `ThemeStore` (`config/kestrel/theme.json`), `ReloadCommand`, `IrisCompat`.
- Per version only: `KestrelClient.java` (render event + command registration; 1.21.11 uses `ClientCommandManager`/`WorldRenderEvents`, 26.x uses `ClientCommands`/`LevelRenderEvents`), `fabric.mod.json`.

## Decisions and facts
- Arrow physics read from game bytecode; identical in 1.21.11 and 26.x (gravity 0.05, air drag 0.99, water drag 0.6, speed = power*3.0, spawn at eye-0.1).
- Lock range = max flight distance at full charge (~143 blocks).
- Drawing uses vanilla Gizmos only. Boxes are depth-tested on purpose (no wallhack on PvP servers).
- Two themes only: `pixel_gradient` (default, formerly `khaopun`: pixel corner brackets, purple-pink-cyan gradients, smooth easing, rainbow only at full charge) and `classic`. Owner likes gradients, cuteness, smoothness.
- Iris does not map the Gizmo point/quad/fan pipelines (`DEBUG_POINTS` etc.), so under a shader pack dots are drawn as small 3-axis line crosses (`compat.points`: auto/lines/points). Lines and text are fine. User confirmed this works with Complementary Unbound.
- Test shader: Complementary Unbound. Owner's own mods for reference: `C:\dev\minecraft\minecraft-mods\hitboxstyle`.

## Build / deploy
`$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'; cd mc-<ver>; .\gradlew.bat build` (JDK 25 needed to run Gradle even for 1.21.11).
Copy `mc-<ver>/build/libs/kestrel-<version>.jar` (version is in `gradle.properties`, now `1.0.0-beta.1`) to the Prism instance mods folder (`%APPDATA%\PrismLauncher\instances\<name>\minecraft\mods`) and delete any older kestrel jar there, or the game refuses to start with a duplicate mod: `PvP 1.21.11` (1.21.11), `Survival 26.3` (26.3), `Survival 26.2` (26.2). Close the game first (jar is locked while running). Old Step 1 jars: `C:\dev\minecraft\disabled-mods\kestrel-step1\`.
Instance notes: ptp removed from the two original instances (jars in `C:\dev\minecraft\disabled-mods\`); `particlerain ...+26.2` disabled on Survival 26.3 (crashes there, unrelated).

## Step 2 progress
See the end of this file (updated as work lands).

### Step 2 status (2026-10-09, code written, builds on all 3 versions, NOT yet tested in game)
Done:
- `core`: `TargetMotion` (10-tick history, classify stationary / constant velocity / erratic), `InterceptSolver` (closed-form ballistic, verified numerically: |v0| = speed and arrow lands on target), `Predictor`/`Prediction`. Pseudocode in `brief/step2-pseudocode.md`. Thresholds in `KestrelSettings`.
- `sight/LockController` (key press logic, unlock rules, motion recording per client tick), `sight/LockKey` (rebindable key, default R, category Kestrel; registrar passed in per version because Fabric's key API class name differs), prediction + alignment test in `SightController.emitPrediction` (trajectory polyline vs ghost box grown by the arrow hit margin).
- Events: TARGET_LOCKED, TARGET_UNLOCKED, TARGET_LOST, GHOST_PREDICTED, AIM_ALIGNED, AIM_MOVING, TARGET_UNREACHABLE; payloads LockInfo / UnlockInfo / GhostInfo.
- Feedback layer + themes: locked frame, aligned (green), unlock (red, `unlock.duration` s), ghost (clear / faded+jitter / unreachable red + "OUT OF REACH"), lock/unlock sounds (khaopun only). Switches `enabled.lock`, `enabled.ghost`.
Known gaps / decisions to confirm with the user:
- The ghost is currently an outline box (plus a translucent fill when no shader pack is active), NOT a translucent render of the target's own model/skin as the brief asks. A model ghost needs a per-version entity-renderer spike (26.x render-state API).
- Prediction ignores vertical movement (target y is kept), so a falling/flying target is predicted at its current height.
- Lock key picks the nearest candidate within 60 degrees of the view direction (brief gave no limit); switch angle 6 degrees. Both in `KestrelSettings`.
- Ping comes from `PlayerInfo.getLatency()`; 0 in singleplayer.
- Acceptance items (8 of 10 hits at 20-30 blocks, strafing = erratic, etc.) need in-game verification by the user.

## Step 3 and prediction fixes (2026-10-10)
- Multi-target prediction: `TargetTracker` keeps motion for every lockable entity while a bow is held; up to `PREDICT_MAX_OTHERS` unlocked candidates get a faint ghost (`GHOST_OTHER`, theme `ghost.others` / `ghost.othersAlpha`).
- Step 3 (Mode B) done and tested in singleplayer. R now toggles Mode A/B (saved in `config/kestrel.properties`); the lock key is gone and the target is picked automatically while drawing. Mode B turns the camera with `ModeB.drive` + `AimSolver`, the mouse is swallowed by `EntityTurnMixin` and used to switch target. Steps 4 (server handshake/homing) and 5 are not started; Step 4 is probably skipped because servers' anti-cheat blocks it.
- **Why shots missed at first:** the intercept maths was right (flight time grows with distance), but it aimed as if the arrow left at the instant of the last frame and as if the client's target position were the real one. In fact the client draws entities about 2-3 ticks behind the server, and the arrow leaves 1-2 ticks after the last frame. So every target ended up about 0.5 block (2-3 ticks of walking) past the aim point, which is as wide as a husk, and the player had to aim at the far edge of the ghost. Found from the `[kestrel-shot]` log (`DebugShots`): `errAlong` was positive in 13 of 19 shots. Fix: `KestrelSettings.SHOT_DELAY_TICKS = 3` is added to the latency in `Predictor`. After the fix the test range shots hit dead centre.
- Test range: `testmap/kestrel_test` datapack (husks that walk back and forth; install in a world's `datapacks` folder; `/function kestrel_test:all_mid|all_slow|all_fast|all_zig|clear`). `DebugShots` logs every shot at a predicted target; remove or switch off when no longer needed.

## Step 5 (2026-10-10): done, tested in game by the owner
- 5a weapons: `sight/HeldWeapon` is the only place that decides what is aimed (bow, loaded crossbow incl. Multishot and firework rockets, trident; Riptide = off). `core/FlightModel` has one model per projectile (arrow, trident, rocket), `ShotProfile` carries speed / fuse / Multishot angles. Pseudocode: `brief/step5-pseudocode.md`.
- 5b sounds: hooks fullCharge, locked, unlocked (also used when the target is lost), aligned, switched, modeA, modeB. All sounds are vanilla sound ids in the themes (clean note-block / amethyst / UI sounds); the owner has no custom sound files. To use their own files later: add a resource pack with sounds and change the ids in the config screen.
- 5c config screen: `config/KestrelConfigScreen` (Cloth Config, opened from Mod Menu; both optional). Visual and sound values are written to the `overrides` of `config/kestrel/theme.json`, behaviour values to `config/kestrel/settings.json` (`SettingsStore`, live values in `KestrelSettings`). `/kestrel reload` re-reads both.
- Skipped on purpose: Step 4 (server handshake and homing): servers' anti-cheat blocks it. So there is no homing switch in the config screen.
- Known gaps: the ghost is an outline, not a translucent copy of the target's model (see Step 2 gaps); the config screen's reset button restores the value from when the screen opened, not the theme default; `SHOT_DELAY_TICKS` (3) was measured in singleplayer with ping 0; the `[kestrel-shot]` log (`debug.shotLog` in settings.json, or the Limits tab) is off by default.

## Step 6 (2026-10-11): release preparation
Done and pushed (commit "Prepare 1.0.0-beta.1"), builds on all 3 versions:
- **Target filter** (`sight/LockFilter`, settings in `KestrelSettings`, file `config/kestrel/settings.json` keys `filter` and `modeBFilter`): categories players / hostile / animals / villagers / other, separate sets for Mode A and Mode B, plus `alwaysIds` / `neverIds` (`minecraft:creeper`, `modid:*`). Category comes from `Enemy`, `AbstractVillager`, `Player`, else `MobCategory`. Defaults: A = players + hostile, B = hostile only.
- **Mode B on servers**: the singleplayer-only restriction is gone. `AimMode.modeBPermitted` = own switch (`modeB.enabled`) AND server policy. One-time chat reminder per session on multiplayer (`AimMode.warnOnce`, off with `modeB.serverWarning`). Chat is written through `AimMode.setChatMessenger`, set per version in `KestrelClient` (1.21.11 `displayClientMessage`, 26.x `sendSystemMessage`).
- **Server rules** (`sight/ServerPolicy`): the client looks for scoreboard teams `ks_off_all`, `ks_off_path`, `ks_off_lock`, `ks_off_predict`, `ks_off_modeb`, `ks_off_players` (every team is synced to clients; no server mod). Hierarchy: all > lock > predict > modeb; `path` and `players` are separate. Gates are in `SightController`, `LockController.onTick`, `LockFilter`, `AimMode`. Datapack `datapacks/kestrel_rules` creates/removes the teams (`/function kestrel_rules:<rule>/off|on`, `preset/*`, `status`). Honour system, not anti-cheat.
- **Head aim** (`ModeB.aimPoint`): head (eye height of the predicted position) if the target is on the ground, not in water, and a block raycast start->head is clear; else hitbox centre; held 0.25 s to avoid flicker. Setting `modeB.aimHead`. The ghost box itself is unchanged (still the whole hitbox).
- Docs and metadata: MIT licence in the root and each `mc-*` folder (they were CC0 before), `README.md`, `README.th.md`, `CHANGELOG.md`, `fabric.mod.json` homepage / sources / issues / description, version `1.0.0-beta.1`.
- `release/`: `build-release.ps1` (builds all versions into `release/dist`, git-ignored), `MODRINTH.md` (page text), `CHECKLIST.md`.

Tested in game (owner, test server `C:\MinecraftServer\KestrelTest262`, Fabric 26.2, superflat, datapack `kestrel_test` + `kestrel_rules`): server rules `modeb`, `predict`, `lock`, `path`, `all` each turn the right feature off and `on` restores it.
**Not tested in game yet:** head aim (esp. a hopping target), the new settings tabs, rule `players/off` (needs a second player), the 26.3 and 1.21.11 jars (they compile only).

Known gaps: prediction ignores vertical target movement; ghost is an outline; `SHOT_DELAY_TICKS` measured at ping 0; the config reset button restores the value from when the screen opened.

### Test server
`C:\MinecraftServer\KestrelTest262` (copy of the Survival262 launcher, only Fabric API in mods, Kestrel is client-only so it is not installed there). Start with `start-server.bat`; console commands from outside via `send-console.ps1 -TargetPid <java pid> -Text "<command>"`. Port 25565 (same as the other servers, only one at a time), public address through the playit tunnel. Offline mode, no whitelist; ops: the owner and the friends who joined. Spawn monsters off, slimes were killed.
