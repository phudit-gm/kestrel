# Kestrel Step 2 — Target lock, motion prediction, ghost model, Mode A

## Objective
The player can lock onto an entity with a key; Kestrel predicts where the locked target will be when the arrow arrives, shows a translucent ghost of the target there, and signals when the player's own aim puts the impact point on the ghost (Mode A).

## Context
- Relevant files/modules: the Kestrel project from Step 1 (`core`, `sight`).
- Current state: not verified by the brief author — see Open questions.

## Scope
**In scope**
- Lock key (rebindable in Controls). Behavior on press:
  - No lock → lock the candidate (from Step 1's filtered, in-range set) nearest to the crosshair.
  - Locked, and crosshair is now nearest to a different candidate within a small angle → switch to that candidate.
  - Otherwise → unlock.
- Automatic unlock when: target dies or is removed; target leaves lock range; target has no line of sight for longer than a timeout (default 2 s); the held item is no longer a bow.
- On any unlock, the target's box turns red for a short time (default 1 s), then disappears.
- Motion tracking in `core`:
  - Keep the target's recent velocity history (default 10 ticks).
  - Classify each tick: stationary, constant velocity, or erratic, using thresholds on speed and velocity variation.
  - Stationary → aim point is the target itself.
  - Constant velocity → solve for the intercept point using the Step 1 simulator and the current draw strength.
  - Erratic → aim at the mean of the target's recent positions.
  - No intercept solution (e.g. target faster than the arrow) → mark as unreachable.
  - Compensate for network latency using the player's current ping when on a server.
  - Same logic for players and mobs; no special-casing by entity type.
- Ghost: a translucent render of the target's own model (player skin for players) at the predicted point.
  - Constant velocity → clear ghost.
  - Erratic → faded ghost with a visible jitter.
  - Unreachable → red ghost plus an "out of reach" label.
- Mode A (the only mode in this step): the player aims manually. When the Step 1 impact point touches the ghost, the ghost and box turn green.
- Lock state, prediction, and ghost update every frame while drawing; the lock persists between draws but is released by the unlock rules above.

**Out of scope**
- Mode B (auto camera), mode toggle key.
- Server handshake, server permissions, arrow homing.
- Crossbow, trident, sounds, config screen.

## Constraints
- Do not modify: Step 1 simulator behavior or its acceptance results.
- Conventions: prediction logic lives in `core` and must not depend on client-only classes (the server reuses it in Step 4). All thresholds and durations as named constants in the settings class.
- Dependencies: no new dependencies.
- Write pseudocode for the motion classifier and the intercept solver before implementing them.

## Acceptance criteria
- [ ] Project still builds and all Step 1 acceptance criteria still pass.
- [ ] Lock key locks, switches, and unlocks exactly as described.
- [ ] Each unlock condition triggers a red box for about 1 s, then the box disappears.
- [ ] A stationary target: ghost overlaps the target.
- [ ] A target moving in a straight line at constant speed (e.g. a second player walking or sprinting, or a mob moving straight): the ghost is ahead of the target along its path, and at full charge, shooting so the impact point is on the ghost hits the target in at least 8 of 10 shots at 20–30 blocks.
- [ ] A target strafing left-right is classified erratic: ghost is faded and jittering near the center of its movement.
- [ ] A target faster than the arrow can reach shows a red ghost and "out of reach".
- [ ] Ghost and box turn green exactly when the impact point touches the ghost.
- [ ] Works on a vanilla dedicated server with no server install. (Step 4 will later restrict Mode A on servers without Kestrel; for now it remains client-only.)

## Open questions
- Confirm the Step 1 project exists, builds, and passes its acceptance criteria before starting; report anything that does not.
- Loader and Minecraft version are whatever Step 1 used; confirm from the project files.
