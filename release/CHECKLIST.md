# Release checklist (1.0.0-beta.1)

Not done by the build; the owner decides and does these.

## Before publishing
- [ ] Test in game and tick off (nothing below has been seen in game yet):
  - [ ] Mode B head aim on the test range (`/function kestrel_test:all_mid`), including a target that hops
  - [ ] Settings screen: Targeting tab, Mode B tab, save and reopen
  - [ ] `players/off` rule with a second player (the other five rules were tested on a server)
  - [ ] 26.3 and 1.21.11 jars: launch the game, draw a bow, press R, open the settings
- [ ] Screenshots / a short GIF (trajectory, boxes, ghost, Mode B) for the page
- [ ] Decide the project slug and name on Modrinth / CurseForge; check "Kestrel" is free
- [ ] Decide if beta.1 is labelled Beta (recommended) or Release

## Build and upload
1. `powershell -File release\build-release.ps1`  ->  `release/dist/` has 3 jars and the datapack zip.
2. Modrinth: one project, one version per Minecraft version (loader Fabric, dependency Fabric API required,
   Cloth Config and Mod Menu optional). Page text: `release/MODRINTH.md`. Changelog: `CHANGELOG.md`.
3. Upload `kestrel_rules-datapack-<version>.zip` as an extra file of each version or as a separate project.
4. Tag the repo: `git tag v1.0.0-beta.1 && git push --tags`, and create a GitHub release with the same files.

## Known limits to say on the page
- Prediction ignores vertical movement of the target; the ghost is an outline, not a copy of the model.
- `SHOT_DELAY_TICKS` was measured in singleplayer; with high ping accuracy is lower.
- Server rules are honour-based, not anti-cheat.
