# Kestrel Step 3 — Mode B (auto camera aim) and mode toggle

## Objective
A second aim mode where, while drawing with a locked target, Kestrel turns the player's camera to the predicted intercept point at a visibly human-fast speed, the mouse switches targets instead of aiming, and control always returns to the player when drawing stops.

## Context
- Relevant files/modules: the Kestrel project from Steps 1–2 (`core`, `sight`).
- Current state: not verified by the brief author — see Open questions.

## Scope
**In scope**
- Mode toggle key (rebindable). Toggles A ↔ B. The chosen mode is saved and persists across draws and game restarts. First run defaults to A.
- Mode B is active only while ALL are true: drawing a bow, a target is locked, Mode B selected, and Mode B is permitted (see below).
- While Mode B is active:
  - The camera rotates toward the Step 2 aim point (intercept / mean position / target) with a configurable maximum angular speed. Default: fast enough to feel like a quick human turn, slow enough to see the motion (about 0.25 s for a 90° turn).
  - The target box flashes while the camera is still rotating, and turns green once aligned.
  - Mouse movement does NOT move the camera. Instead, raw mouse movement is accumulated; when it exceeds a threshold (configurable), Kestrel switches to the candidate lying in that on-screen direction from the current target, resets the accumulator, and applies a short cooldown.
- Camera control returns to the player immediately when: draw is released or cancelled, target is unlocked or lost, the held item changes, any screen/GUI opens, or the player dies.
- Camera rotation uses the normal client-side player rotation so the server sees ordinary rotation updates.
- Permission in this step: Mode B is permitted in singleplayer only. On any multiplayer server, Mode B is unavailable and the toggle stays in A (Step 4 replaces this with server-granted permission).

**Out of scope**
- Server handshake and permissions (Step 4).
- Arrow homing.
- A "flick on release" aim variant — explicitly not wanted.
- Crossbow, trident, sounds, config screen.

## Constraints
- Do not modify: Step 1–2 behavior in Mode A.
- Conventions: Mode B logic in `sight`; it reads aim points from `core` and must not duplicate prediction math. Speed, threshold, and cooldown as named constants in the settings class.
- Dependencies: no new dependencies.
- Write pseudocode for the Mode B state machine (enter, rotate, aligned, switch, exit) before implementing it.

## Acceptance criteria
- [ ] Project builds and Step 1–2 acceptance criteria still pass in Mode A.
- [ ] Toggle key switches modes; mode survives releasing the bow and restarting the game; fresh install starts in A.
- [ ] In singleplayer, Mode B turns the camera to the aim point with visible motion (not an instant snap), box flashes during the turn and turns green when aligned.
- [ ] Moving the mouse in Mode B never moves the camera; moving it past the threshold toward another candidate switches to that candidate.
- [ ] Each exit condition returns full mouse control immediately, verified one by one.
- [ ] Holding a bow without drawing, or holding any other item, never locks the mouse.
- [ ] On a multiplayer server, Mode B cannot be selected and Mode A works as before.

## Open questions
- Confirm Steps 1–2 exist, build, and pass their acceptance criteria before starting; report anything that does not.
