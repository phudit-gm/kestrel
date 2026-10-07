package com.khaopun.kestrel;

/**
 * Every colour, range and limit used by Kestrel, in one place.
 * Step 5 will turn the filter values into a config screen; until then they are plain constants.
 * Colours are ARGB.
 */
public final class KestrelSettings {
	// --- Trajectory preview ---
	/** Safety cap on simulated ticks per frame. */
	public static final int TRAJECTORY_MAX_TICKS = 300;
	public static final int TRAJECTORY_COLOR = 0xFFFFFFFF;
	public static final int TRAJECTORY_COLOR_FULL_CHARGE = 0xFF55FFFF;
	public static final float TRAJECTORY_LINE_WIDTH = 2.0F;
	/** Drawing only: the line starts at the bow hand and slides onto the real path, so the arc reads from behind. */
	public static final double HAND_OFFSET_SIDE = 0.4D;
	public static final double HAND_OFFSET_DOWN = 0.25D;
	/** Number of simulated ticks over which the hand offset fades to zero. */
	public static final int HAND_OFFSET_FADE_TICKS = 12;
	public static final int IMPACT_COLOR = 0xFFFF5555;
	public static final float IMPACT_MARKER_HALF_SIZE = 0.15F;
	public static final float IMPACT_MARKER_LINE_WIDTH = 3.0F;

	// --- Full charge indicator ---
	public static final String FULL_CHARGE_TEXT = "FULL";
	public static final int FULL_CHARGE_TEXT_COLOR = 0xFF55FFFF;
	/** How far in front of the camera the indicator floats, and how far below the crosshair line. */
	public static final double FULL_CHARGE_TEXT_DISTANCE = 2.5D;
	public static final double FULL_CHARGE_TEXT_DROP = 0.5D;
	/** Vertical gap between stacked crosshair labels (FULL, then TARGET). */
	public static final double CROSSHAIR_LABEL_ROW_GAP = 0.2D;

	// --- Lockable entity boxes ---
	public static final int MAX_LOCK_BOXES = 32;
	public static final int LOCK_BOX_COLOR = 0xFFFFFF00;
	public static final int TARGET_BOX_COLOR = 0xFF00FF00;
	public static final float LOCK_BOX_LINE_WIDTH = 2.0F;
	/** Boxes are grown by this much so they never z-fight with the vanilla F3+B hitbox. */
	public static final double LOCK_BOX_INFLATE = 0.08D;
	public static final String TARGET_TEXT = "TARGET";
	public static final int TARGET_TEXT_COLOR = 0xFF00FF00;
	public static final double TARGET_TEXT_HEIGHT_ABOVE_BOX = 0.35D;

	// --- Lock filter (Step 5 will expose these in a config screen) ---
	public static final boolean LOCK_HOSTILE_MOBS = true;
	public static final boolean LOCK_PLAYERS = true;
	public static final boolean LOCK_EXCLUDE_VILLAGERS = true;
	public static final boolean LOCK_EXCLUDE_TAMED = true;

	private KestrelSettings() {
	}
}
