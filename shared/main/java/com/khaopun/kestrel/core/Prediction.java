package com.khaopun.kestrel.core;

import net.minecraft.world.phys.Vec3;

/**
 * Where the locked target is expected to be when the arrow arrives.
 *
 * @param aim    predicted entity position (same reference point as the entity's own position)
 * @param kind   how reliable the prediction is
 * @param motion how the target has been moving
 */
public record Prediction(Vec3 aim, Kind kind, MotionClass motion) {
	public enum Kind {
		/** Stationary or constant velocity: a solid prediction. */
		CLEAR,
		/** Erratic movement: aim at the centre of the recent movement. */
		FADED,
		/** No arrow can reach the target (it outruns the arrow or is too far). */
		UNREACHABLE
	}
}
