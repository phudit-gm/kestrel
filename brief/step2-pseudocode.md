# Step 2 pseudocode: motion classifier and intercept solver

Required by the Step 2 brief before implementing. Lives in `core` (no client classes). Units: blocks, ticks. All thresholds are named constants in `KestrelSettings`.

## Motion tracker (per locked target, fed once per client tick)

```
state: positions[ ] (ring buffer, newest last, size HISTORY_TICKS + 1)

record(pos):
  append pos; drop oldest if over size

velocities():   // horizontal only (y is ignored: ground mobs/players stay on the ground)
  for i in 1..n-1: v[i] = (positions[i].xz - positions[i-1].xz)      // blocks per tick

classify():
  if v.count < MIN_SAMPLES:                       return STATIONARY    // not enough history: assume it stays
  meanSpeed = average(|v[i]|)
  if meanSpeed < STATIONARY_SPEED:                return STATIONARY
  vMean = average(v[i])                           // vector mean
  variation = average(|v[i] - vMean|) / meanSpeed // 0 = perfectly constant, ~1+ = erratic
  if variation <= CONSTANT_MAX_VARIATION:         return CONSTANT_VELOCITY
  return ERRATIC
```

A strafing target alternates sign, so `|v[i] - vMean|` is about `|v[i]|` and `variation` is near 1: erratic. A target walking straight has `v[i]` close to `vMean`: variation near 0.

## Prediction (every frame while drawing)

```
predict(target, drawPower, start (arrow launch point), latencyTicks):
  switch classify():
    STATIONARY:        aim = target.pos;                          result = (aim, CLEAR)
    ERRATIC:           aim = mean(positions);                     result = (aim, FADED)
    CONSTANT_VELOCITY: intercept = solveIntercept(start, speed, target.pos, vMean, latencyTicks)
                       if none:                                   result = UNREACHABLE (ghost stays at target.pos)
                       else aim = intercept.point;                result = (aim, CLEAR)
```

## Intercept solver (ballistic model of the Step 1 arrow, no collisions)

The arrow's tick is: `pos += vel; vel *= AIR_DRAG; vel.y -= GRAVITY`. Closed form after `n` ticks from launch velocity `v0`:

```
A(n) = sum_{k=0}^{n-1} AIR_DRAG^k = (1 - AIR_DRAG^n) / (1 - AIR_DRAG)
G(n) = vertical displacement from gravity only = sum over ticks, computed by the same loop (v0 = 0)
pos(n) = start + v0 * A(n) + (0, G(n), 0)
```

The target at flight time `t` (plus latency) is `P(t) = p0 + vMean * (t + latencyTicks)` (horizontal only; y = p0.y).
To hit `P(t)` at tick `t`, the launch velocity must be `v0(t) = (P(t) - start - (0, G(t), 0)) / A(t)`, and a real shot has `|v0| = speed` (speed = power * SPEED_PER_POWER).

```
f(t) = |v0(t)| - speed
solveIntercept:
  for t = 1 .. MAX_FLIGHT_TICKS (integer scan, step 1):
    if f(t-1) > 0 and f(t) <= 0 (first crossing, the flat low arc):   // f > 0: needs more speed than we have
       refine t in [t-1, t] by bisection (fractional ticks, ~20 iterations, A and G evaluated for real n)
       return Intercept(point = P(t), flightTicks = t)
  return NONE                                   // never reachable inside MAX_FLIGHT_TICKS: unreachable
```

If `f(1) <= 0` already (target adjacent), the answer is `t = 1`. A target outrunning the arrow keeps `f(t) > 0` for all `t`, so there is no solution: unreachable.

## Latency

`latencyTicks = pingMillis / 50`. We see the target about half a ping late and the server sees our arrow about half a ping later, so the target will have moved about one full ping beyond what we see when the arrow spawns. The solver adds `latencyTicks` to the target's extrapolation time. Singleplayer: 0.

## Alignment (Mode A)

```
aligned = any segment of the Step 1 trajectory polyline (up to the impact point)
          intersects  ghostBox  = target.boundingBox moved to aim, inflated by ENTITY_HIT_MARGIN
```
