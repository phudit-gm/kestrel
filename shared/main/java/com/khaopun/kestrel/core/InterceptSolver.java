package com.khaopun.kestrel.core;

import com.khaopun.kestrel.KestrelSettings;

import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Finds where to shoot at a target moving with constant velocity, using the arrow's ballistic motion (the same
 * gravity and drag as {@link ProjectileSimulator}, without collisions). See {@code brief/step2-pseudocode.md}.
 *
 * <p>After n ticks from launch velocity v0 the arrow is at
 * {@code start + v0 * A(n) + (0, G(n), 0)} with {@code A(n) = (1 - d^n) / (1 - d)} and
 * {@code G(n) = -g / (1 - d) * (n - A(n))}, where d is the air drag and g the gravity.
 */
public final class InterceptSolver {
	private static final int BISECTIONS = 30;
	/** Smallest flight time tried; at t = 0 the arrow can only be at the start. */
	private static final double MIN_FLIGHT_TICKS = 0.05D;

	private InterceptSolver() {
	}

	/**
	 * @param start        arrow launch point
	 * @param speed        launch speed in blocks per tick (draw power * SPEED_PER_POWER)
	 * @param target       target position now
	 * @param velocity     target velocity in blocks per tick (horizontal; the y component is ignored)
	 * @param latencyTicks extra ticks the target moves before the shot lands (ping)
	 * @return the first (flattest) intercept, or null if the arrow can never catch the target
	 */
	public static @Nullable Intercept solve(Vec3 start, double speed, Vec3 target, Vec3 velocity, double latencyTicks) {
		return solve(start, speed, target, velocity, latencyTicks, FlightModel.ARROW,
				KestrelSettings.INTERCEPT_MAX_FLIGHT_TICKS);
	}

	/** Same, for any projectile; {@code maxTicks} is the longest flight to consider. */
	public static @Nullable Intercept solve(Vec3 start, double speed, Vec3 target, Vec3 velocity, double latencyTicks,
			FlightModel model, int maxTicks) {
		boolean tooSlowBefore = true;
		for (int t = 1; t <= maxTicks; t++) {
			double f = excessSpeed(start, speed, target, velocity, latencyTicks, model, t);
			if (tooSlowBefore && f <= 0.0D) {
				double lo = Math.max(t - 1, MIN_FLIGHT_TICKS);
				double hi = t;
				for (int i = 0; i < BISECTIONS; i++) {
					double mid = (lo + hi) / 2.0D;
					if (excessSpeed(start, speed, target, velocity, latencyTicks, model, mid) > 0.0D) {
						lo = mid;
					} else {
						hi = mid;
					}
				}
				return new Intercept(targetAt(target, velocity, latencyTicks, hi), hi);
			}
			tooSlowBefore = f > 0.0D;
		}
		return null;
	}

	/** Launch speed an arrow needs to be at the target after {@code t} ticks, minus the speed available. */
	private static double excessSpeed(Vec3 start, double speed, Vec3 target, Vec3 velocity, double latencyTicks,
			FlightModel model, double t) {
		double a = model.travelFactor(t);
		double g = model.gravityDrop(t);
		Vec3 p = targetAt(target, velocity, latencyTicks, t);
		Vec3 v0 = new Vec3((p.x - start.x) / a, (p.y - start.y - g) / a, (p.z - start.z) / a);
		return v0.length() - speed;
	}

	private static Vec3 targetAt(Vec3 target, Vec3 velocity, double latencyTicks, double t) {
		double ahead = t + latencyTicks;
		return new Vec3(target.x + velocity.x * ahead, target.y, target.z + velocity.z * ahead);
	}
}
