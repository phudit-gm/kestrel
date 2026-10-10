package com.khaopun.kestrel;

/**
 * Behaviour constants that are not part of how Kestrel looks or sounds. Everything visual or audible (colours,
 * sizes, durations, sound ids, limits on effects) lives in the theme files; see {@code feedback.theme}.
 * Values marked volatile are the player's settings: they are read from config/kestrel/settings.json and edited in the config screen.
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
	public static volatile int LOCK_NO_LINE_OF_SIGHT_TICKS = 40;

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
	/**
	 * Ticks between the position the client sees and the moment the arrow actually meets the target: the client
	 * draws entities a few ticks behind the server, and the shot leaves one to two ticks after the last frame.
	 * Measured with the [kestrel-shot] log: the target ended up about this far ahead of the aim point.
	 */
	public static final double SHOT_DELAY_TICKS = 3.0D;
	/** Milliseconds per tick, used to turn ping into ticks. */
	public static final double MILLIS_PER_TICK = 50.0D;

	/** How many unlocked candidates (nearest first) are predicted besides the locked one. */
	public static volatile int PREDICT_MAX_OTHERS = 8;

	// --- Mode B (Step 3) ---
	/** Camera turn speed in Mode B. 360 degrees per second is about 0.25 s for a 90 degree turn. */
	public static volatile double MODE_B_TURN_DEG_PER_SEC = 360.0D;
	/** The camera counts as aligned when less than this many degrees are left to turn. */
	public static final double MODE_B_ALIGNED_DEGREES = 0.5D;
	/** Mouse movement (degrees of turn) that switches target in Mode B. */
	public static volatile double MODE_B_SWITCH_THRESHOLD_DEGREES = 6.0D;
	/** Ticks after a switch during which the mouse cannot switch again. */
	public static volatile int MODE_B_SWITCH_COOLDOWN_TICKS = 10;
	/** The accumulated mouse movement is multiplied by this every tick, so slow drift never adds up. */
	public static final double MODE_B_ACCUM_DECAY = 0.9D;
	/** Mode B aims for the head when the target stands on the ground and the head is in the open; otherwise the middle of the hitbox. */
	public static volatile boolean MODE_B_AIM_HEAD = true;
	/** Seconds the aim point stays on head or body before it may change again, so a hopping target does not make the camera jitter. */
	public static final double MODE_B_AIM_SWITCH_SECONDS = 0.25D;
	/** Passes of the aim solver. */
	public static final int AIM_SOLVER_ITERATIONS = 6;

	/** Logs where each shot at a predicted target really landed ([kestrel-shot] in latest.log). Off by default. */
	public static volatile boolean DEBUG_SHOT_LOG = false;

	// --- Lock filter: which kinds of entity may be targeted ---
	// Mode A (preview) and Mode B (camera turns by itself) have their own sets, so Mode B can be stricter.
	public static volatile boolean LOCK_PLAYERS = true;
	public static volatile boolean LOCK_HOSTILE_MOBS = true;
	public static volatile boolean LOCK_ANIMALS = false;
	public static volatile boolean LOCK_VILLAGERS = false;
	public static volatile boolean LOCK_OTHER = false;
	public static volatile boolean LOCK_EXCLUDE_TAMED = true;
	public static volatile boolean MODE_B_LOCK_PLAYERS = false;
	public static volatile boolean MODE_B_LOCK_HOSTILE_MOBS = true;
	public static volatile boolean MODE_B_LOCK_ANIMALS = false;
	public static volatile boolean MODE_B_LOCK_VILLAGERS = false;
	public static volatile boolean MODE_B_LOCK_OTHER = false;
	/** Entity ids (minecraft:creeper, or modid:* for a whole mod) that are always targetable in both modes. */
	public static volatile java.util.List<String> LOCK_ALWAYS_IDS = java.util.List.of();
	/** Entity ids that are never targetable; wins over the always list. */
	public static volatile java.util.List<String> LOCK_NEVER_IDS = java.util.List.of();

	// --- Mode B availability ---
	/** Master switch for Mode B on this client. A server can switch it off too (see ServerPolicy). */
	public static volatile boolean MODE_B_ENABLED = true;
	/** Show the fair-play warning in chat the first time Mode B is chosen on a multiplayer server each session. */
	public static volatile boolean MODE_B_SERVER_WARNING = true;

	private KestrelSettings() {
	}
}
