# Kestrel Step 5 — Crossbow, trident, sound hooks, config screen

## Objective
Kestrel's preview, lock, Modes A/B, and homing work with crossbows (including Multishot and firework rockets) and tridents, play configurable sounds on key events, and expose all player settings in an in-game config screen.

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
  - Sound hooks for: target locked, target unlocked/lost, full charge reached.
  - Each hook plays a sound from a configurable sound id; ship with placeholder vanilla sounds until custom files are provided. Volume per hook configurable; each hook can be disabled.
- Config screen (client) exposing every player setting created in Steps 1–5: box/ghost/marker colors for every state (yellow, green, red, flashing), max box count, entity filters (players, hostile mobs, villagers, tamed animals), line-of-sight unlock timeout, red display duration, Mode B turn speed, Mode B switch threshold and cooldown, homing on/off, sound settings.

**Out of scope**
- Other projectile weapons beyond bow, crossbow, trident.
- Custom sound files (provided later by the user).
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
- [ ] Each sound hook plays, can change sound id and volume, and can be disabled.
- [ ] Every setting listed in Scope appears in the config screen, saves, and applies without restarting the game.

## Open questions
- Confirm Steps 1–4 exist, build, and pass their acceptance criteria before starting; report anything that does not.
- Which config-screen library to use for the project's loader (e.g. the loader's common choice)? Ask before adding it.
