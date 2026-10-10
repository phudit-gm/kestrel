package com.khaopun.kestrel.core;

import com.khaopun.kestrel.KestrelSettings;

import net.minecraft.world.phys.Vec3;

/** Finds the look direction that makes a free-flying arrow pass through a point. Pure maths: no client classes. */
public final class AimSolver {
	private static final int MAX_TICKS = 200;

	private AimSolver() {
	}

	/**
	 * @param start  arrow launch point
	 * @param speed  launch speed (power * speed per power)
	 * @param motion the shooter's own motion that is added to the arrow's velocity
	 * @param point  where the arrow should pass
	 * @return {yaw, pitch} in degrees, in the game's rotation convention
	 */
	public static float[] solve(Vec3 start, double speed, Vec3 motion, Vec3 point) {
		return solve(start, speed, motion, point, FlightModel.ARROW);
	}

	public static float[] solve(Vec3 start, double speed, Vec3 motion, Vec3 point, FlightModel model) {
		Vec3 target = point;
		for (int i = 0; i < KestrelSettings.AIM_SOLVER_ITERATIONS; i++) {
			float[] angles = anglesTo(target.subtract(start));
			Vec3 velocity = ArrowPhysics.direction(angles[1], angles[0]).scale(speed).add(motion);
			Vec3 error = point.subtract(closestApproach(start, velocity, point, model));
			if (error.length() < 0.02D) {
				break;
			}
			target = target.add(error);
		}
		return anglesTo(target.subtract(start));
	}

	/** {yaw, pitch} in degrees of a direction, the inverse of {@link ArrowPhysics#direction}. */
	public static float[] anglesTo(Vec3 d) {
		double yaw = Math.toDegrees(Math.atan2(-d.x, d.z));
		double pitch = -Math.toDegrees(Math.asin(Math.max(-1.0D, Math.min(1.0D, d.y / Math.max(1.0E-9D, d.length())))));
		return new float[] {(float) yaw, (float) pitch};
	}

	private static Vec3 closestApproach(Vec3 start, Vec3 velocity, Vec3 point, FlightModel model) {
		Vec3 pos = start;
		Vec3 vel = velocity;
		Vec3 best = start;
		double bestDist = Double.MAX_VALUE;
		for (int tick = 0; tick < MAX_TICKS; tick++) {
			Vec3 next = pos.add(vel);
			Vec3 seg = next.subtract(pos);
			double len2 = seg.lengthSqr();
			double t = len2 < 1.0E-9D ? 0.0D : Math.max(0.0D, Math.min(1.0D, point.subtract(pos).dot(seg) / len2));
			Vec3 q = pos.add(seg.scale(t));
			double d = q.distanceToSqr(point);
			if (d < bestDist) {
				bestDist = d;
				best = q;
			}
			pos = next;
			vel = vel.scale(model.airDrag()).add(0.0D, -model.gravity(), 0.0D);
		}
		return best;
	}
}
