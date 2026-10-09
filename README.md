# Kestrel

Client-side Fabric mod for Minecraft Java. While you draw a bow it shows the predicted arrow trajectory, an impact marker, boxes around lockable entities in range (turning green with `TARGET` when the arrow's path hits them) and a full-charge indicator. No server install needed.

| Minecraft | Java | Directory |
|-----------|------|-----------|
| 1.21.11   | 21   | `mc-1.21.11` |
| 26.2      | 25   | `mc-26.2` |
| 26.3      | 25   | `mc-26.3` |

`shared/` holds the source used by all versions: physics in `core`, bow-sight logic in `sight`, and `feedback` (events, theme, drawing, sound, particles).

## Look and feel (themes)
Logic only announces events; `feedback` decides how they look and sound from a theme. Two themes ship: `khaopun` (default) and `classic` (the Step 1 look). Edit `config/kestrel/theme.json` (created on first launch) and run `/kestrel reload` in game.

- `theme`: `khaopun` or `classic`
- `enabled`: switches for `trajectory`, `impact`, `candidates`, `hit`, `fullCharge`, `particles`, `sounds`
- `animation` (e.g. `{"enabled": false}` for instant changes, `{"speed": 0.5}`), `limits`
- `overrides`: dotted paths over the theme, e.g. `"candidate.color": "#FFD60080"`, `"trajectory.style": "line"`

Every available key is listed in `shared/main/resources/assets/kestrel/themes/classic.json`; design notes in `brief/draft-kestrel-step1.5-theme-design.md`.

Build (needs JDK 25 to run Gradle): `.\gradlew.bat build` inside a version directory. Jar: `build/libs/kestrel-1.0.0.jar`.

Status: Step 1 done; Step 1.5 (feedback layer) implemented, builds on all versions, in-game testing pending.
