package com.khaopun.kestrel.core;

import java.util.ArrayList;
import java.util.List;

/** Launch constants of the weapons, read from the game's code (BowItem, CrossbowItem, TridentItem). */
public final class WeaponPhysics {
	/** CrossbowItem.ARROW_POWER. */
	public static final double CROSSBOW_ARROW_SPEED = 3.15D;
	/** CrossbowItem.FIREWORK_POWER. */
	public static final double CROSSBOW_ROCKET_SPEED = 1.6D;
	/** TridentItem.PROJECTILE_SHOOT_POWER. */
	public static final double TRIDENT_SPEED = 2.5D;
	/** TridentItem.THROW_THRESHOLD_TIME: a trident held for less than this many ticks is not thrown. */
	public static final int TRIDENT_MIN_TICKS = 10;
	/** The Multishot enchantment's spread between neighbouring projectiles, in degrees. */
	public static final double MULTISHOT_ANGLE = 10.0D;
	/** Rocket fuse: 10 ticks per flight level plus 0..11 random; the middle of that is used. */
	public static final int ROCKET_TICKS_PER_LEVEL = 10;
	public static final int ROCKET_MIDDLE_EXTRA_TICKS = 6;

	private WeaponPhysics() {
	}

	/**
	 * Angles of the side projectiles of a Multishot crossbow, as ProjectileWeaponItem#shoot places them. The centre
	 * projectile (angle 0) is not listed.
	 */
	public static List<Double> spreadAngles(int projectileCount) {
		if (projectileCount <= 1) {
			return List.of();
		}
		double step = 2.0D * MULTISHOT_ANGLE / (projectileCount - 1);
		double offset = (projectileCount - 1) % 2 * step / 2.0D;
		double direction = 1.0D;
		List<Double> out = new ArrayList<>();
		for (int i = 0; i < projectileCount; i++) {
			double angle = offset + direction * ((i + 1) / 2) * step;
			direction = -direction;
			if (i > 0) {
				out.add(angle);
			}
		}
		return out;
	}
}
