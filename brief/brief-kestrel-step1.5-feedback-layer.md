# Kestrel Step 1.5 — Feedback layer (visual style, animation, particles, sound hooks)

## Objective
All of Kestrel's on-screen and audio feedback is produced by one theme-driven feedback layer that matches the look of the player's installed visual mods, with every color, shape, animation, and effect configurable from a single theme file and individually switchable, so later steps add feedback only through this layer.

## Context
- Relevant files/modules: the Kestrel repository from Step 1 (`core`, `sight`). The repository root contains `mc-1.21.11/`, `mc-26.2/`, `mc-26.3/`, and `shared/`. Step 1 is complete.
- Current state: Step 1 draws the trajectory line, yellow target boxes, green `TARGET` boxes, and the full-charge indicator directly in `sight`. The author reports the default look does not fit the player's other mods (shader/animation/cute-UI themed visuals). The player's installed mods, shader pack, and resource pack have not been inspected by the brief author.

## Scope
**In scope**
- A feedback layer that sits between game logic and rendering/audio:
  - Logic code (`core`, `sight` selection/lock/aim code) only announces named events. It never chooses colors, shapes, particles, or sounds.
  - The feedback layer subscribes to events and decides what to show or play.
- Define the full event list now, even where logic does not exist yet: trajectory visible, impact point, target candidate in range, target under trajectory (hit), full charge reached, target locked, target unlocked, target lost, aim aligned, aim moving, target unreachable. Only wire visuals and sounds for events that exist in Step 1: trajectory, impact point, candidate in range, target under trajectory, full charge. Later steps connect the rest.
- Move every existing Step 1 drawing into the feedback layer.
- One theme file (human-editable, in the loader's standard config location) controlling:
  - Colors for every state (trajectory, candidate box, hit box, full-charge indicator), including opacity.
  - Shapes: trajectory style (solid line, dashed line, beads, pixel dots), target frame style (corner brackets, rounded corners, full box), sizes and thicknesses.
  - Animation: transition speed and easing between states, fade in/out, small bounce on state change, option to disable all animation.
  - Distance fade: trajectory and effects fade near the player so they do not block the view.
  - Limits: max particles, max effect updates per second.
- Individual on/off switches: trajectory line, impact marker, candidate boxes, hit box + `TARGET` label, full-charge indicator, particles, sounds.
- Ship two themes selectable by name in the theme file: `khaopun` (default: pixel corner brackets, bead trajectory, purple-pink-cyan gradients, soft smooth animation, rainbow only while fully charged) and `classic` (the Step 1 look). Both use the same layer; no theme-specific code outside the theme data and its renderers. Full design: `brief/draft-kestrel-step1.5-theme-design.md`.
- `/kestrel reload` client command re-reads the theme file without restarting the game.
- Supported versions: 1.21.11, 26.2, 26.3.
- Particles are created client-side only, never on the server.
- Sound hooks: the sound system with per-event sound id, volume, and on/off, using vanilla sounds as placeholders for locked, unlocked, and full-charge events (only full-charge has a trigger in this step; the others stay unwired). Custom sound files are not part of this step.
- Compatibility: rendering must remain visible and correctly layered with a shader pack enabled and with other hitbox-display mods enabled.

**Out of scope**
- Ghost model, red/flashing states, Mode A/B, locking, prediction (Steps 2–3).
- A player-facing in-game config screen (Step 5); the theme file is edited by hand for now.
- Custom sound files.
- Any server-side behavior or networking.
- Redesigning trajectory physics.

## Constraints
- Do not modify: `core` physics and its Step 1 acceptance results.
- Conventions: no color, size, duration, or sound id may be written as a literal in rendering or logic code outside the theme/default-theme data; a search of the source must show none. New feedback in later steps must be added only as new events plus theme entries.
- Dependencies: no new dependencies.
- Write pseudocode for the event → feedback dispatch flow before implementing it.

## Acceptance criteria
- [ ] Project builds on all three versions (`mc-1.21.11`, `mc-26.2`, `mc-26.3`); all Step 1 acceptance criteria still pass.
- [ ] `/kestrel reload` applies theme-file edits without restarting.
- [ ] Switching the theme name in the theme file changes the trajectory style, frame style, colors, and animation without code changes, for both themes (`khaopun`, `classic`).
- [ ] Editing any single color, size, or speed in the theme file changes only that element.
- [ ] Each on/off switch disables exactly its own element and nothing else.
- [ ] Setting "disable animation" removes all easing and bounce: state changes are instant.
- [ ] Trajectory and effects fade near the player and do not obscure the crosshair.
- [ ] A source search finds no literal color, size, duration, or sound-id values in rendering or logic code outside the theme data.
- [ ] Triggering the full-charge event plays the configured placeholder sound at the configured volume; disabling sounds silences it.
- [ ] With the player's shader pack enabled, the trajectory, boxes, and indicator remain visible and not washed out; verified in-game with Complementary Unbound (the player's shader pack).
- [ ] With another hitbox-display mod enabled, Kestrel's frames and that mod's boxes are both visible without flicker.
- [ ] With 100+ mobs nearby, effects stay within the configured particle and update limits and the frame rate shows no noticeable drop.

## Open questions
- The feedback layer must work in `mc-1.21.11`, `mc-26.2` and `mc-26.3`. Confirm what `shared/` contains and whether the layer's version-independent parts (events, theme data) belong there, before implementing.
- RESOLVED: visual mods, shader and packs were read from the player's Prism instances; findings in the draft design file. Shader: Complementary Unbound.
- Can the agent launch the game and see the rendered result? If not, ask the player to try each starter theme in-game and report which fits, before tuning further.
- RESOLVED: default theme is `khaopun`.
