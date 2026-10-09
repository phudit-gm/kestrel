# Kestrel hand-off (2026-10-09)

## State
Step 1 and Step 1.5 are DONE and tested in game by the user (themes, `/kestrel reload`, shader pack, other hitbox mods: all fine).
Step 2 (lock, prediction, ghost, Mode A) is IN PROGRESS: see `brief/brief-kestrel-step2-lock-prediction-modeA.md` and the "Step 2 progress" section below.
Repo: https://github.com/phudit-gm/kestrel (branch `main`). Commits are local until the user asks to push.

## Layout
- `mc-1.21.11/`, `mc-26.2/`, `mc-26.3/`: separate Fabric Gradle projects (1.21.11 / Java 21; 26.2 and 26.3 / Java 25). Each adds `shared/main/java`, `shared/main/resources` and `shared/client/java` as source dirs in `build.gradle`.
- `shared/main/java`:
  - `core`: ArrowPhysics, ProjectileSimulator, LockRange, TrajectoryResult (Step 1 physics, unchanged).
  - `feedback`: `FeedbackEvent` (all events incl. later ones), `FeedbackBus` (level-triggered: emit every active event each frame, then `frameEnd()`), payload records, `theme/` (`Theme`, `ThemeLoader`, `ColorSpec`, `Easing`).
  - `KestrelSettings`: only behaviour constants (lock filter, simulation cap). Visual/audio values live in themes.
- `shared/main/resources/assets/kestrel/themes/`: `classic.json` (Step 1 look; lists every key; also the fallback for missing keys) and `khaopun.json` (default).
- `shared/client/java`: `sight/SightController` (logic only, emits events), `sight/LockFilter`, `feedback/FeedbackLayer` (draws/plays from the theme, per-element animated tracks), `ThemeStore` (`config/kestrel/theme.json`), `ReloadCommand`, `IrisCompat`.
- Per version only: `KestrelClient.java` (render event + command registration; 1.21.11 uses `ClientCommandManager`/`WorldRenderEvents`, 26.x uses `ClientCommands`/`LevelRenderEvents`), `fabric.mod.json`.

## Decisions and facts
- Arrow physics read from game bytecode; identical in 1.21.11 and 26.x (gravity 0.05, air drag 0.99, water drag 0.6, speed = power*3.0, spawn at eye-0.1).
- Lock range = max flight distance at full charge (~143 blocks).
- Drawing uses vanilla Gizmos only. Boxes are depth-tested on purpose (no wallhack on PvP servers).
- Two themes only: `khaopun` (default: pixel corner brackets, purple-pink-cyan gradients, smooth easing, rainbow only at full charge) and `classic`. Owner likes gradients, cuteness, smoothness.
- Iris does not map the Gizmo point/quad/fan pipelines (`DEBUG_POINTS` etc.), so under a shader pack dots are drawn as small 3-axis line crosses (`compat.points`: auto/lines/points). Lines and text are fine. User confirmed this works with Complementary Unbound.
- Test shader: Complementary Unbound. Owner's own mods for reference: `C:\dev\minecraft\minecraft-mods\hitboxstyle`.

## Build / deploy
`$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'; cd mc-<ver>; .\gradlew.bat build` (JDK 25 needed to run Gradle even for 1.21.11).
Copy `mc-<ver>/build/libs/kestrel-1.0.0.jar` to the Prism instance mods folder as `kestrel-1.0.0.jar`: `PvP 1.21.11` (1.21.11), `Survival 26.3` (26.3), `Survival 26.2` (26.2). Close the game first (jar is locked while running). Old Step 1 jars: `C:\dev\minecraft\disabled-mods\kestrel-step1\`.
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
