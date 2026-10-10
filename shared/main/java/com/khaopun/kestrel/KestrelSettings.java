package com.khaopun.kestrel;

/**
 * Behaviour constants that are not part of how Kestrel looks or sounds. Everything visual or audible (colours,
 * sizes, durations, sound ids, limits on effects) lives in the theme files; see {@code feedback.theme}.
 * Step 5 will turn the filter values into a config screen; until then they are plain constants.
 */
public final class KestrelSettings {
	/** Safety cap on simulated ticks per frame. */
	public static final int TRAJECTORY_MAX_TICKS = 300;

	// --- Target lock (Step 2) ---
	/** The lock key picks the candidate nearest the crosshair, but only within this angle of the view direction. */
	public static final double LOCK_MAX_ANGLE_DEGREES = 60.0D;
	/** While locked, the key switches to another candidate only if the crosshair is within this angle of it. */
	public static final double LOCK_SWITCH_ANGLE_DEGREES = 6.0D;
	/** Unlock after the target has had no line of sight for this many ticks. */
	public static final int LOCK_NO_LINE_OF_SIGHT_TICKS = 40;

	// --- Motion tracking and prediction (Step 2) ---
	/** Number of recent ticks of target movement kept. */
	public static final int TARGET_HISTORY_TICKS = 10;
	/** Fewer movement samples than this and the target is assumed stationary. */
	public static final int MOTION_MIN_SAMPLES = 4;
	/** Mean horizontal speed (blocks per tick) below which a target counts as stationary. */
	public static final double STATIONARY_SPEED = 0.01D;
	/** Mean deviation from the average velocity, relative to speed, above which movement counts as erratic. */
	public static final double CONSTANT_MAX_VARIATION = 0.35D;
	/** Longest arrow flight (ticks) the intercept solver looks at. */
	public static final int INTERCEPT_MAX_FLIGHT_TICKS = 200;
	/** Milliseconds per tick, used to turn ping into ticks. */
	public static final double MILLIS_PER_TICK = 50.0D;

	/** How many unlocked candidates (nearest first) are predicted besides the locked one. */
	public static final int PREDICT_MAX_OTHERS = 8;

	// --- Mode B (Step 3) ---
	/** Camera turn speed in Mode B. 360 degrees per second is about 0.25 s for a 90 degree turn. */
	public static final double MODE_B_TURN_DEG_PER_SEC = 360.0D;
	/** The camera counts as aligned when less than this many degrees are left to turn. */
	public static final double MODE_B_ALIGNED_DEGREES = 0.5D;
	/** Mouse movement (degrees of turn) that switches target in Mode B. */
	public static final double MODE_B_SWITCH_THRESHOLD_DEGREES = 6.0D;
	/** Ticks after a switch during which the mouse cannot switch again. */
	public static final int MODE_B_SWITCH_COOLDOWN_TICKS = 10;
	/** The accumulated mouse movement is multiplied by this every tick, so slow drift never adds up. */
	public static final double MODE_B_ACCUM_DECAY = 0.9D;
	/** Passes of the aim solver. */
	public static final int AIM_SOLVER_ITERATIONS = 6;

	// --- Lock filter (Step 5 will expose these in a config screen) ---
	public static final boolean LOCK_HOSTILE_MOBS = true;
	public static final boolean LOCK_PLAYERS = true;
	public static final boolean LOCK_EXCLUDE_VILLAGERS = true;
	public static final boolean LOCK_EXCLUDE_TAMED = true;

	private KestrelSettings() {
	}
}
