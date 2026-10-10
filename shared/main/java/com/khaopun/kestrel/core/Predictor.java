package com.khaopun.kestrel.core;

import com.khaopun.kestrel.KestrelSettings;

import net.minecraft.world.phys.Vec3;

/** Turns a target's tracked motion into a {@link Prediction}. Pure maths: no client classes. */
public final class Predictor {
	private Predictor() {
	}

	/**
	 * @param motion     the target's recorded movement (must not be empty)
	 * @param start      arrow launch point
	 * @param power      current draw power (0..1)
	 * @param pingMillis round-trip latency to the server, 0 in singleplayer
	 * @param bodyHeight height above the entity position that the arrow should hit (about half the hitbox height)
	 */
	public static Prediction predict(TargetMotion motion, Vec3 start, float power, double pingMillis, double bodyHeight) {
		return predict(motion, start, power * ArrowPhysics.SPEED_PER_POWER, FlightModel.ARROW,
				KestrelSettings.INTERCEPT_MAX_FLIGHT_TICKS, pingMillis, bodyHeight);
	}

	/**
	 * @param speed    launch speed in blocks per tick
	 * @param model    how the projectile flies
	 * @param maxTicks longest flight to consider
	 */
	public static Prediction predict(TargetMotion motion, Vec3 start, double speed, FlightModel model, int maxTicks,
			double pingMillis, double bodyHeight) {
		Vec3 now = motion.latest();
		MotionClass motionClass = motion.classify();
		return switch (motionClass) {
			case STATIONARY -> new Prediction(now, Prediction.Kind.CLEAR, motionClass);
			case ERRATIC -> new Prediction(motion.meanPosition(), Prediction.Kind.FADED, motionClass);
			case CONSTANT_VELOCITY -> {
				Vec3 body = new Vec3(0.0D, bodyHeight, 0.0D);
				Intercept hit = InterceptSolver.solve(start, speed, now.add(body), motion.meanVelocity(),
						pingMillis / KestrelSettings.MILLIS_PER_TICK + KestrelSettings.SHOT_DELAY_TICKS, model, maxTicks);
				yield hit == null ? new Prediction(now, Prediction.Kind.UNREACHABLE, motionClass)
						: new Prediction(hit.point().subtract(body), Prediction.Kind.CLEAR, motionClass);
			}
		};
	}
}
