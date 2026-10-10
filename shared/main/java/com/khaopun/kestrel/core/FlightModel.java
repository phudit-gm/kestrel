package com.khaopun.kestrel.core;

/**
 * How a projectile flies after it leaves the weapon. Each weapon has its own model, read from the game's code
 * (AbstractArrow, ThrownTrident, FireworkRocketEntity). Client-independent.
 *
 * @param gravity   subtracted from the Y velocity every tick
 * @param airDrag   velocity multiplier per tick outside water (1 = none)
 * @param waterDrag velocity multiplier per tick in water (1 = none)
 * @param spawnDrop how far below the shooter's eye the projectile appears
 */
public record FlightModel(double gravity, double airDrag, double waterDrag, double spawnDrop) {
	/** Arrows from bows and crossbows. */
	public static final FlightModel ARROW = new FlightModel(ArrowPhysics.GRAVITY, ArrowPhysics.AIR_DRAG,
			ArrowPhysics.WATER_DRAG, ArrowPhysics.SPAWN_EYE_OFFSET);
	/** ThrownTrident: same as an arrow, but its water inertia is 0.99 instead of 0.6. */
	public static final FlightModel TRIDENT = new FlightModel(ArrowPhysics.GRAVITY, ArrowPhysics.AIR_DRAG, 0.99D,
			ArrowPhysics.SPAWN_EYE_OFFSET);
	/**
	 * A firework rocket shot from a crossbow ("shot at angle"): it keeps its launch velocity in a straight line, with
	 * no gravity and no drag, and appears 0.15 below the eye.
	 */
	public static final FlightModel ROCKET = new FlightModel(0.0D, 1.0D, 1.0D, 0.15D);

	/** Sum of the drag factors over the first {@code t} ticks: position after t ticks = start + v0 * this. */
	public double travelFactor(double t) {
		return airDrag == 1.0D ? t : (1.0D - Math.pow(airDrag, t)) / (1.0D - airDrag);
	}

	/** Vertical displacement from gravity alone after {@code t} ticks (zero or negative). */
	public double gravityDrop(double t) {
		if (gravity == 0.0D) {
			return 0.0D;
		}
		if (airDrag == 1.0D) {
			return -gravity * t * (t - 1.0D) / 2.0D;
		}
		return -gravity / (1.0D - airDrag) * (t - travelFactor(t));
	}
}
