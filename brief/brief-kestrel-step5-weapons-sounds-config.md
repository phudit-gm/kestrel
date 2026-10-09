# Kestrel Step 5 — Crossbow, trident, sound files, config screen

## Objective
Kestrel's preview, lock, Modes A/B, and homing work with crossbows (including Multishot and firework rockets) and tridents, the player's custom sound files replace the placeholder sounds, and all player settings are exposed in an in-game config screen.

## Context
- Relevant files/modules: the Kestrel project from Steps 1–4.
- Current state: not verified by the brief author — see Open questions.

## Scope
**In scope**
- Crossbow:
  - Loaded crossbow held → preview, lock, Mode A/B active immediately while held (no draw phase). In Mode B with a locked target, the mouse is controlled while the loaded crossbow is held; all Step 3 exit conditions still apply.
  - Unloaded crossbow → nothing active until loading completes.
  - Multishot → render all three trajectories; lock, prediction, ghost, and Mode B use the center projectile only.
  - Firework rocket ammo → separate physics model in `core` for rocket flight; preview and prediction use it.
  - Homing (Step 4) applies to crossbow arrows; firework rockets are excluded from homing.
- Trident:
  - Uses the draw phase like a bow, with the trident's own throw speed, gravity, and drag — including its lower drag in water — read from the game's code.
  - If the trident has Riptide AND the player is in water or rain (the condition where Riptide launches the player instead of throwing), disable preview, lock, Mode A, and Mode B entirely, and never control the camera.
  - Homing applies to thrown tridents the same way as arrows.
- Sounds:
  - The sound system and event hooks already exist from Step 1.5. In this step, add sound hooks for any events Steps 2–4 introduced that are still unwired, and replace placeholder vanilla sounds with the custom sound files the player provides.
  - Each hook keeps a configurable sound id, volume, and on/off switch.
- Config screen (client) exposing every player setting created in Steps 1–5, including everything in the Step 1.5 theme file: theme selection, colors for every state (yellow, green, red, flashing), shapes, animation speed and on/off, per-element on/off switches, particle limits, max box count, entity filters (players, hostile mobs, villagers, tamed animals), line-of-sight unlock timeout, red display duration, Mode B turn speed, Mode B switch threshold and cooldown, homing on/off, sound settings. The screen edits the same theme/settings data the files use; it must not introduce a second source of truth.

**Out of scope**
- Other projectile weapons beyond bow, crossbow, trident.
- Creating or sourcing sound files (the player provides them).
- Server config changes beyond adding firework-rocket and trident handling to existing feature switches.

## Constraints
- Do not modify: bow behavior and all Step 1–4 acceptance results.
- Conventions: each weapon's physics as its own model in `core` behind a common interface; weapon detection in one place; settings stored in the loader's standard config location.
- Dependencies: only the loader, its standard API, and the config-screen library listed in Open questions once decided.
- Write pseudocode for weapon detection (including the Riptide condition) before implementing it.

## Acceptance criteria
- [ ] Project builds; Steps 1–4 acceptance criteria still pass with a bow.
- [ ] Loaded crossbow shows preview immediately; unloaded crossbow shows nothing until loaded.
- [ ] Multishot crossbow shows three trajectories; lock and ghost follow the center one.
- [ ] Firework-rocket crossbow: predicted impact within 1.5 blocks of the real impact at 20 blocks on flat ground.
- [ ] Trident: predicted impact within 1 block of the real impact on flat ground at full charge, and in water the predicted path matches the trident's longer underwater travel.
- [ ] Riptide trident in water or rain: no preview, no lock, no camera control; outside water and rain it behaves like a normal trident.
- [ ] Each sound hook plays the player-provided file (or its placeholder if none is provided), can change sound id and volume, and can be disabled.
- [ ] Every setting listed in Scope appears in the config screen, saves, and applies without restarting the game.

## Open questions
- Confirm Steps 1–4 exist, build, and pass their acceptance criteria before starting; report anything that does not.
- Which config-screen library to use for the project's loader (e.g. the loader's common choice)? Ask before adding it.
- Where are the player's custom sound files? If none are provided yet, keep placeholders and report which hooks still use them.
