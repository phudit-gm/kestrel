package com.khaopun.kestrel.core;

import net.minecraft.world.phys.Vec3;

/**
 * The furthest an arrow fired at full charge can get from the shooter, computed with the same drag and gravity
 * as the real arrow (no blocks, no water). It is relative to the launch point, so it is a constant for a given
 * game version and is computed once.
 */
public final class LockRange {
	/** How far below the launch point a flight keeps counting (a target can stand lower than the shooter). */
	private static final double MAX_DROP = 32.0D;
	private static final int MAX_TICKS = 400;
	private static final float PITCH_STEP = 1.0F;

	private static final double FULL_CHARGE_RANGE = compute();

	private LockRange() {
	}

	/** Maximum straight-line distance an arrow at full charge can reach from the player. */
	public static double fullCharge() {
		return FULL_CHARGE_RANGE;
	}

	private static double compute() {
		double speed = WeaponPhysics.CROSSBOW_ARROW_SPEED; // the fastest arrow of any weapon
		double best = 0.0D;
		for (float pitch = -90.0F; pitch <= 90.0F; pitch += PITCH_STEP) {
			Vec3 vel = ArrowPhysics.direction(pitch, 0.0F).scale(speed);
			Vec3 pos = Vec3.ZERO;
			for (int tick = 0; tick < MAX_TICKS && pos.y > -MAX_DROP; tick++) {
				pos = pos.add(vel);
				vel = vel.scale(ArrowPhysics.AIR_DRAG).add(0.0D, -ArrowPhysics.GRAVITY, 0.0D);
				best = Math.max(best, pos.length());
			}
		}
		return best;
	}
}
