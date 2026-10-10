# Step 3 pseudocode: auto target, Mode B state machine, aim solver

Decisions made with the owner (2026-10-10): prediction is always on (every candidate is predicted while drawing); the lock key is gone; R now toggles Mode A / Mode B; the target is chosen automatically.

## Auto target (LockController, every client tick)
```
if not drawing a bow:            target = none (silent, no red box); return
if target and (dead | out of range | no line of sight too long):  announce TARGET_LOST; target = none
best = candidate with smallest angle to the view direction
if no target:                    if best within LOCK_MAX_ANGLE: target = best
else if mode B is not in effect: if best != target and angle(best) <= LOCK_SWITCH_ANGLE: target = best   // follows the crosshair
else (mode B):                   target is sticky; only the mouse flick changes it
```

## Mode B state machine
```
active(now) = mode == B and singleplayer and drawing a bow and target exists and no screen open and player alive
              (evaluated live, so every exit condition releases the mouse in the same tick)

mouse turn event (mixin on Entity.turn, local player only):
  if active: accum += (dYaw, dPitch) in degrees; cancel the turn      // camera never follows the mouse
  else:      let it through

each client tick, if active:
  cooldown -= 1; accum *= DECAY
  if cooldown <= 0 and |accum| >= SWITCH_THRESHOLD:
     dir = unit(accum)
     pick the candidate (not the target) whose (dYaw, dPitch) from the target, seen from the eye, has
     cos(angle to dir) >= 0.5, smallest angular distance
     if found: target = it, cooldown = SWITCH_COOLDOWN
     accum = 0

each frame, if active and prediction reachable:
  (yaw, pitch) = AimSolver(start, speed, playerMotion, ghostCentre)
  step = TURN_SPEED * dt
  rotate (yaw, pitch) toward the solution by at most `step` degrees, along the shortest way
  turning = angle left after this step > ALIGNED_DEG   -> event AIM_TURNING (box flashes)
  aligned colour comes from AIM_ALIGNED (the path touches the ghost)
```

## AimSolver (core, free flight, no blocks)
```
T = P                                  // P = point to hit
repeat ITERATIONS:
   (yaw, pitch) = angles(T - start)
   vel = direction(yaw, pitch) * speed + playerMotion
   Q = point of the simulated path (drag 0.99, gravity 0.05) closest to P
   e = P - Q
   if |e| < 0.02: break
   T = T + e                           // aim a bit past the miss
return angles(T - start)
```
