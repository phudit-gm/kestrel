# Kestrel Step 4 — Client/server handshake, server permissions, server-side arrow homing

## Objective
When Kestrel is installed on a server, the server grants each modded player the features the server owner allows, and arrows shot by permitted players with homing enabled steer smoothly toward their locked target; on servers without Kestrel, only the trajectory preview works.

## Context
- Relevant files/modules: the Kestrel project from Steps 1–3 (`core`, `link`, `sight`, `flight`).
- Current state: not verified by the brief author — see Open questions.

## Scope
**In scope**
- `link` handshake on join:
  - Client announces it has Kestrel (with mod version).
  - Server replies with the allowed feature set: preview, Mode A, Mode B, homing.
  - No reply within a timeout (vanilla or non-Kestrel server) → only preview is allowed; Mode A, Mode B, and homing are disabled on the client.
  - Singleplayer (integrated server) → all features allowed.
- Server config file, editable by the server owner, with an on/off switch for each feature (preview, Mode A, Mode B, homing). Applied on join and on server reload/restart.
- Client setting "homing on/off" (default off), sent to the server; homing applies only if the player turned it on AND the server allows it.
- Client sends the currently locked target's entity id to the server at fire time (and on lock change), so the server knows what each arrow should chase.
- `flight` (server side), for each arrow fired by a permitted player with homing on and a locked target:
  - Every tick, re-predict the intercept point using `core` with the server's own entity data, and steer toward that point (not the target's current position).
  - No steering for the first few ticks after firing; then steering strength ramps up.
  - Maximum turn angle per tick (configurable).
  - Steering strength scales with prediction confidence: higher for erratic targets, lower for constant-velocity targets.
  - If a block is between the arrow and the target, stop steering until line of sight returns.
  - Stop steering when the target dies, is removed, or the arrow hits something.
  - While steering, send the arrow's updated velocity to all tracking clients every tick so it does not jitter on screen.
- Arrows from players without Kestrel, without permission, or with homing off behave exactly like vanilla.
- Vanilla clients (no mod) can join a Kestrel server and see all arrows normally.

**Out of scope**
- Crossbow, trident, sounds, client config screen (Step 5).
- Pathfinding arrows around walls.
- Any new items, entities, or registry entries.

## Constraints
- Do not modify: vanilla arrow entity class definitions; steering is applied to existing arrow entities' velocity.
- Conventions: all networking in `link`; all steering in `flight`; `flight` reuses `core` prediction and must not reimplement it. Server-side values (turn limit, ramp, confidence scaling) as named constants with server config overrides.
- Dependencies: only the loader's standard networking API.
- Server must trust only its own entity data for steering; the client supplies only the target id.
- Write pseudocode for the handshake and for the per-tick steering loop before implementing them.

## Acceptance criteria
- [ ] Project builds; Steps 1–3 acceptance criteria still pass in singleplayer.
- [ ] On a vanilla dedicated server: preview works; Mode A, Mode B, and homing are unavailable.
- [ ] On a Kestrel dedicated server with all features on: Mode A, Mode B, and homing are available to a modded client.
- [ ] Turning off any single feature in the server config makes exactly that feature unavailable after rejoin.
- [ ] A vanilla client can join the Kestrel server, shoot normally, and sees homing arrows from other players fly without visible jitter.
- [ ] With homing on, against a target walking a straight line at 30 blocks, at least 9 of 10 arrows hit.
- [ ] With homing on, against a strafing target at 20 blocks, the hit rate is clearly higher than with homing off (record both over 10 shots each).
- [ ] Homing arrows curve gradually: no turn larger than the configured per-tick limit (log turn angles in a debug build to verify).
- [ ] A homing arrow does not steer into a wall between it and the target.
- [ ] Arrows from a player with homing off, or from a non-permitted player, match vanilla behavior.

## Open questions
- Confirm Steps 1–3 exist, build, and pass their acceptance criteria before starting; report anything that does not.
- Should the server config also support per-player permissions (e.g. only some players may use homing), or one setting for everyone?
