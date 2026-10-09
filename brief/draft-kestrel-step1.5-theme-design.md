# Kestrel Step 1.5 — DRAFT: adjustable theme design + "Khaopun" signature theme

Status: draft for the owner to edit. Companion to `brief-kestrel-step1.5-feedback-layer.md` (answers its open questions on the player's mods, adds one theme, fixes the config format).

## 1. What the owner's setup says (read from Prism instances, 2026-10-09)

Instances read: `PvP 1.21.11`, `Survival 26.3`.

| Area | Found | What it means for Kestrel |
|---|---|---|
| Own mod `hitboxstyle` | `TargetBoxHud`: red **pixel-grid corner brackets** (15x15 grid, `#` = pixel) around the crosshair on an entity | The owner already likes bracket frames drawn from a pixel grid, not full boxes. Reuse the idea: frame style = pixel pattern. |
| `custom-block-highlight` | **Rainbow** outline, alpha 128, width 2.5, `speed`/`saturation`/`brightness`/`delay` settings | The owner likes slow hue-cycling lines, semi-transparent. Theme needs a `rainbow` color mode with the same four knobs. |
| `combat-hitboxes` | white hitbox, red target box, magenta hurt, blue look-dir, width 2.5 / 3.5 at 32 blocks | Existing box colors to avoid clashing with: white, red, magenta, blue. Kestrel's yellow/green/cyan already sits clear of these. |
| `fancycrosshair` | pixel-art crosshair, hit animation on | Pixel look is the HUD baseline. |
| Resource packs (26.3) | Faithful 32x, Fresh Animations, Animated Hotbar RGB, Rainbow XP Bar, Dark Purple Glint, Dramatic Skys | Vanilla-faithful base + **RGB/rainbow animated UI** + **dark purple** accent. |
| Shaders | Complementary Unbound (selected; the owner has a `+linewidth` build, so lines were a problem before), also Reimagined, BSL, Reverie, Photon, Solas, etc. | Lines must stay visible under bloom/tonemapping: need a minimum opacity and optional width boost. Test with Unbound first. |
| Other | Elytra Trails, Visuality, Particular, Iris, Sodium (+Extra), Entity Culling | Particles must be few and cheap; Entity Culling can hide boxes of culled mobs — expect no boxes for hidden entities (intended). |

Look read from files, not from names. The in-game result still needs the owner's eyes (see section 6).

## 2. Direction: two themes, `khaopun` (default) and `classic`

`classic` reproduces the Step 1 look (current `KestrelSettings` values) and is the safe fallback. The owner's theme is the **default**:

**`khaopun`** — "cute pixel brackets + soft gradients + smooth motion, rainbow only at full charge"

Owner's stated tastes (2026-10-09): likes **gradients**, **cuteness**, and **smoothness**. Rainbow only when fully charged; everything else as first proposed.

- Gradients everywhere instead of flat colors: every element's color may be a 2–3 stop gradient (see schema). Default palette: dark purple → pink → soft cyan.
- Candidate frame: pixel corner brackets (from the hitboxstyle grid idea), semi-transparent, purple→pink gradient along each bracket arm. Cute touch: the bracket corners are rounded by one pixel (the grid omits the outer corner pixel).
- Hit frame / `TARGET`: same brackets, solid and brighter (pink→white), small soft bounce on entry (`outBack`).
- Trajectory: beads (small square dots, slightly larger near the middle of the arc, tapering at both ends so it reads cute and round) with a purple→cyan gradient from hand to impact. Fades near the player. **Rainbow only while fully charged**: the gradient switches to a rainbow that flows along the arc.
- Impact marker: small pixel cross, dark purple core with a bright pink edge (readable under bloom).
- Full-charge: the crosshair-side `FULL` text pulses once, then holds, in rainbow.
- Smoothness: no hard snaps. All state changes ease (`outCubic`/`outBack`, 0.12–0.25 s), colors and opacity interpolate, frames fade in and out instead of popping, and bead positions are interpolated per frame (not per tick).
- Sounds: off by default except full-charge (quiet, vanilla placeholder; pick a soft chime).

The look is derived from the owner's setup and stated tastes; not yet seen in game.

## 3. "Adjustable" — config design

Format: JSON (same as the owner's other mod configs; Gson, no new dependency).
Location: `config/kestrel/theme.json` (user file) + bundled themes read from the jar as `assets/kestrel/themes/<name>.json`.

Resolution order (later wins): bundled theme `<name>` → user `overrides` block. Changing one value in `overrides` changes only that element. Deleting `overrides` returns to the pure theme. Missing or invalid values fall back to the bundled theme value and log one warning; the game never crashes on a bad theme file.

Reload: `/kestrel reload` (client command) and on file save if cheap; otherwise on world join. Saves restarting the game for every tweak.

```jsonc
{
  "theme": "khaopun",            // khaopun | classic
  "enabled": {                   // the seven switches from the brief
    "trajectory": true, "impact": true, "candidates": true,
    "hit": true, "fullCharge": true, "particles": true, "sounds": true
  },
  "animation": { "enabled": true, "speed": 1.0 },   // speed multiplies all theme durations
  "limits":    { "maxParticles": 64, "maxUpdatesPerSecond": 30 },
  "overrides": {                 // anything from the theme schema below; only what you list changes
    "elements.candidate.color": "#FFD60080",
    "elements.trajectory.width": 3.0
  }
}
```

Dotted keys in `overrides` keep the user file short and diff-friendly. Colors: `"#RRGGBB"`, `"#RRGGBBAA"`, or a rainbow object.

### Theme schema (per element; same shape for every element)

```jsonc
"elements": {
  "trajectory": { "style": "beads|line|dashed|pixels", "color": "#FFFFFF", "width": 2.0,
                  "spacing": 0.6, "distanceFade": { "start": 1.5, "end": 4.0 } },
  "impact":     { "style": "cross|ring|pixelCross", "color": "...", "size": 0.15 },
  "candidate":  { "frame": "corners|rounded|box|pixelCorners", "color": "...", "width": 2.0, "inflate": 0.08 },
  "hit":        { "frame": "...", "color": "...", "label": { "text": "TARGET", "color": "..." } },
  "fullCharge": { "text": "FULL", "color": "...", "distance": 2.5, "drop": 0.5 }
}
"color": "#55FFFFC0"
"color": { "mode": "gradient", "stops": ["#5B2A86", "#FF7AC6", "#7FF0FF"],   // along the element (arm, arc)
           "flow": 0.0 }                                          // >0 makes the gradient scroll
"colorWhileFullCharge": { "mode": "rainbow", ... }                  // optional per-element override by state
"color": { "mode": "rainbow", "speed": 7.5, "saturation": 1.0, "brightness": 1.0,
           "delay": 250, "alpha": 128 }                         // same knobs as custom-block-highlight
"states": { "candidate->hit": { "duration": 0.12, "easing": "outBack", "bounce": 0.08 } }
"pixelGrids": { "corner": ["##...##", "#.....#", ...] }         // '#' = drawn pixel, like TargetBoxHud
"sounds": { "fullCharge": { "id": "minecraft:block.note_block.chime", "volume": 0.4, "pitch": 1.2 } }
```

Every value above lives only in theme JSON; `KestrelSettings` constants move into the bundled `khaopun`/`classic` data (the brief's "no literals" rule).

### Where code goes (answers brief open question 1)

- `shared/main`: `FeedbackEvent` enum (full list), `FeedbackBus`, theme data model + loader + validation (no Minecraft rendering types).
- `shared/client`: `FeedbackLayer` (subscribes to the bus), element renderers (trajectory, frame, label), particle/sound adapters.
- Per version (`mc-*`): only the render hook registration, as in Step 1.
- Logic (`core`, `sight` selection code) calls `FeedbackBus.emit(EVENT, payload)` and nothing else.

## 4. Event → feedback dispatch (pseudocode, required by the brief)

```
onClientTick():
  state = FeedbackState(prev, current)          // set of active events + payloads
  for each event in current not in prev: layer.enter(event)
  for each event in prev not in current: layer.exit(event)

layer.enter(event):
  for el in theme.bindings[event]:             // e.g. TARGET_UNDER_TRAJECTORY -> [hitFrame, label, sound]
    if !switch[el.group]: continue
    el.transition.start(from=el.current, to=el.target, theme.states[...], animation.enabled)
    if el.sound and switch.sounds: play(el.sound)
    if el.particle and switch.particles and budget.take(): spawnClientParticle(el.particle)

onRender(frame):                                // per frame
  for el in activeElements: draw(el, el.transition.value(now), theme, distanceFade(camera))
```

The event → element bindings (`theme.bindings`) are theme data, so adding feedback later means a new event plus a binding, never a renderer edit.

## 5. Events

Defined now: TRAJECTORY_VISIBLE, IMPACT_POINT, CANDIDATE_IN_RANGE, TARGET_UNDER_TRAJECTORY, FULL_CHARGE, TARGET_LOCKED, TARGET_UNLOCKED, TARGET_LOST, AIM_ALIGNED, AIM_MOVING, TARGET_UNREACHABLE.
Wired in 1.5: the first five. The rest are declared with no emitters.

## 6. Questions for the owner (answers change the default theme)

Answered 2026-10-09: rainbow only at full charge; the rest of `khaopun` as proposed; likes gradients, cuteness, smoothness.

Decided 2026-10-09:
- Test shader: Complementary Unbound.
- `/kestrel reload` client command is in scope.
- Ship exactly two themes: `khaopun` (default) and `classic` (the current Step 1 look). The brief's `soft`/`clean`/`pixel` are dropped.
- Targets: 1.21.11, 26.2 and 26.3 (`mc-26.2/` added; it is a copy of the 26.3 project with Minecraft/Fabric API 26.2 and `~26.2` in `fabric.mod.json`, and it builds with the unchanged Step 1 sources).

Still open:
1. Gradient palette: purple → pink → cyan is a guess. Other colors you want?

## 7. Not decided / risks

- Pixel-grid frames around a 3D entity: the grid has to be projected to screen space (like `TargetBoxHud`) or mapped onto the box corners in world space. World-space corner brackets are simpler and shader-safe; screen-space pixel grids look more like the owner's existing mod. Needs a quick spike in game.
- Gizmos (Step 1) have fixed line styles; beads/pixel dots and per-vertex rainbow may require our own quad rendering. Check what Gizmos can do before promising it.
- Not verified in game; nothing here has been built.
