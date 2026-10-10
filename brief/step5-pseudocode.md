# Step 5 pseudocode: weapon detection and per-weapon physics

Weapon detection lives in one place: `sight/HeldWeapon`. Every other class (preview, lock, ghost, Mode B, target tracking) asks it for the current `ShotProfile` and never looks at items itself. The physics of each projectile is a `FlightModel` in `core`.

Numbers below were read from the game's code (26.3 sources): `CrossbowItem`, `TridentItem`, `ProjectileWeaponItem`, `Projectile`, `AbstractArrow`, `ThrownTrident`, `FireworkRocketEntity`.

## Detection
```
current(player):
  if player is using an item:
    bow:      power = powerForTime(ticksUsing + partial)
              if power < MIN_POWER: none
              shot = arrow model, speed = power * 3.0, adds shooter motion
    trident:  if Riptide level > 0 (it launches the player instead of throwing; Riptide only throws never):  none
              if ticksUsing < 10 (THROW_THRESHOLD_TIME):                                                       none
              shot = trident model, speed = 2.5, adds shooter motion
    anything else (a crossbow being loaded, food, a shield): none
  else for hand in [main hand, off hand]:
    crossbow that is charged:
       projectiles = charged contents
       spread     = Multishot angles (see below), empty for a single projectile
       if the contents are a firework rocket:
            shot = rocket model, speed = 1.6, fuse = 10 * (1 + flight level) + 6 ticks, no shooter motion
       else shot = arrow model, speed = 3.15, no shooter motion
  none otherwise
```

Why Riptide is always "none": `TridentItem#releaseUsing` only spawns a thrown trident when the Riptide level is 0. With Riptide the player is launched, and only in water or rain (outside it, `use` fails and the player cannot even start drawing). The brief's rule (no preview/lock/camera control when Riptide and in water or rain) therefore holds, and so does "not Riptide, not in water" because the trident cannot be used at all.

## Flight models
```
arrow:   gravity 0.05, air drag 0.99, water drag 0.6,  appears 0.1 below the eye   (bow and crossbow arrows)
trident: gravity 0.05, air drag 0.99, water drag 0.99, appears 0.1 below the eye   (water inertia is the only difference)
rocket:  gravity 0,    air drag 1,    water drag 1,    appears 0.15 below the eye   (crossbow rockets are "shot at angle":
                                                                                    no acceleration, no gravity, straight line)
tick:    pos += vel; vel *= drag (air or water by the position); vel.y -= gravity
```
Closed form used by the intercept solver for n ticks (drag d, gravity g): `travel(n) = (1 - d^n) / (1 - d)` (or `n` if d = 1) and `drop(n) = -g / (1 - d) * (n - travel(n))` (or `-g * n (n - 1) / 2` if d = 1, 0 if g = 0).

## Multishot
```
count = number of loaded projectiles          // 3 with Multishot
step   = 2 * 10 degrees / (count - 1)
offset = ((count - 1) % 2) * step / 2
for i in 0..count-1:  angle(i) = offset + direction * ((i + 1) / 2) * step ; direction flips every projectile
// count = 3 gives 0, +10, -10: i = 0 is the centre projectile.
```
The side projectiles are rotated around the player's up axis. They are drawn as extra trajectories (`TRAJECTORY_EXTRA`); lock, ghost, alignment and Mode B use the centre projectile only.

## Crossbow differences to the bow
- No draw phase: everything is active as soon as a charged crossbow is held (main hand first, then off hand).
- The crossbow calls `Projectile#shoot`, not `shootFromRotation`, so the shooter's own movement is **not** added to the projectile.
- Speed is fixed (3.15 arrows, 1.6 rockets) and the shot is always "fully charged".
