# Changelog

## 1.0.0-beta.1

First public beta. Minecraft 1.21.11, 26.2, 26.3.

- Trajectory and impact point for bow, crossbow (Multishot, firework rockets) and trident.
- Target boxes, lead prediction ("ghost") that accounts for ping.
- Mode A (preview) and Mode B (camera turns to the aim point). R switches. Mode B aims for the head when the
  target is on the ground and the head is in the open, otherwise the middle of the hitbox.
- Mode B works on servers. A reminder about server rules is shown in chat the first time it is used. It can be
  turned off in the settings, and so can Mode B itself.
- Choose what can be targeted (players, hostile mobs, animals, villagers, other) separately for Mode A and Mode B,
  with always/never lists by entity id. Animals and villagers are off by default.
- Servers can switch features off with scoreboard teams (`ks_off_*`); datapack `kestrel_rules` included.
- Settings screen (Cloth Config + Mod Menu), themes `pixel_gradient` (default, formerly `khaopun`) and `classic`.
- Opt-in shot log for testing.
- In the settings screen, the reset button on each option goes back to the shipped default, also after Save and reopening.
