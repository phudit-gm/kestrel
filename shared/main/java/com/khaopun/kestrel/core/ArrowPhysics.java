package com.khaopun.kestrel.core;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Arrow constants and launch maths, read from the game's own code
 * (AbstractArrow, BowItem, Projectile) in Minecraft 1.21.11 and 26.3 - they are identical in both.
 * Client-independent: safe to use from server code later.
 */
public final class ArrowPhysics {
	/** AbstractArrow#getDefaultGravity: subtracted from the Y velocity every tick. */
	public static final double GRAVITY = 0.05D;
	/** AbstractArrow#getAirDrag / INERTIA: velocity multiplier per tick when not in water. */
	public static final double AIR_DRAG = 0.99D;
	/** AbstractArrow#getWaterInertia: velocity multiplier per tick when in water. */
	public static final double WATER_DRAG = 0.6D;
	/** ProjectileWeaponItem: bow launch speed = power * this (blocks per tick). */
	public static final float SPEED_PER_POWER = 3.0F;
	/** Arrow spawn point is the shooter's eye minus this (AbstractArrow owner constructor). */
	public static final double SPAWN_EYE_OFFSET = 0.1D;
	/** ProjectileUtil margin used when sweeping the arrow against entity boxes. */
	public static final double ENTITY_HIT_MARGIN = 0.3D;
	/** BowItem#getPowerForTime is capped at this value (fully charged). */
	public static final float FULL_CHARGE_POWER = 1.0F;
	/** BowItem#releaseUsing: below this power no arrow is fired. */
	public static final float MIN_POWER = 0.1F;

	private static final float DEG_TO_RAD = 0.017453292F;

	private ArrowPhysics() {
	}

	/** Same formula as BowItem#getPowerForTime, but with fractional ticks so it can update every frame. */
	public static float powerForTime(float chargeTicks) {
		float f = chargeTicks / 20.0F;
		f = (f * f + f * 2.0F) / 3.0F;
		return Math.min(f, 1.0F);
	}

	/** Unit look direction exactly as Projectile#shootFromRotation builds it (uses Mth's sin/cos tables). */
	public static Vec3 direction(float xRot, float yRot) {
		double x = -Mth.sin(yRot * DEG_TO_RAD) * Mth.cos(xRot * DEG_TO_RAD);
		double y = -Mth.sin(xRot * DEG_TO_RAD);
		double z = Mth.cos(yRot * DEG_TO_RAD) * Mth.cos(xRot * DEG_TO_RAD);
		return new Vec3(x, y, z).normalize();
	}
}
