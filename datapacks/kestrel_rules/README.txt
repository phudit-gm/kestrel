Kestrel server rules (datapack)
================================

Kestrel is a client mod. Players who have it can switch Mode B (automatic camera aim), the predicted-position
ghost, target boxes and more. This datapack lets you, the server owner, turn those features off.

How it works: each rule is a scoreboard team with a reserved name (ks_off_modeb, ...). The game sends every team
to every client, and Kestrel hides the matching feature while the team exists. No server mod, no plugin: it works
on vanilla, Paper, Purpur, Fabric, Forge ... as long as datapacks work. A plugin can do the same by running the
same /team commands.

IMPORTANT: this is a request the mod honours, not anti-cheat. A player who modifies the mod or does not install it
is not affected. Keep your normal anti-cheat rules.

Install
  Put the kestrel_rules folder in <world>/datapacks and run /reload (or restart).

Use (operators)
  /function kestrel_rules:modeb/off        no Mode B
  /function kestrel_rules:modeb/on         Mode B allowed again
  /function kestrel_rules:predict/off      no ghost and no lead prediction (also no Mode B)
  /function kestrel_rules:lock/off         no target boxes or locking (also no prediction, no Mode B)
  /function kestrel_rules:path/off         no trajectory line or impact point
  /function kestrel_rules:players/off      cannot target players
  /function kestrel_rules:all/off          Kestrel does nothing
  (every rule also has /on)

Presets (set all rules at once)
  /function kestrel_rules:preset/open              everything allowed
  /function kestrel_rules:preset/no_modeb          everything except Mode B
  /function kestrel_rules:preset/no_aim_assist     boxes + trajectory only (no ghost, no Mode B)
  /function kestrel_rules:preset/pvp_safe          no ghost, no Mode B, players cannot be targeted
  /function kestrel_rules:preset/trajectory_only   trajectory line only
  /function kestrel_rules:preset/off               Kestrel does nothing

Check what is on: /function kestrel_rules:status  (teams named ks_off_* are the rules that are switched off)

Do not use these team names for anything else.
