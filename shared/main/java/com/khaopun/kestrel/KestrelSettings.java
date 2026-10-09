package com.khaopun.kestrel;

/**
 * Behaviour constants that are not part of how Kestrel looks or sounds. Everything visual or audible (colours,
 * sizes, durations, sound ids, limits on effects) lives in the theme files; see {@code feedback.theme}.
 * Step 5 will turn the filter values into a config screen; until then they are plain constants.
 */
public final class KestrelSettings {
	/** Safety cap on simulated ticks per frame. */
	public static final int TRAJECTORY_MAX_TICKS = 300;

	// --- Lock filter (Step 5 will expose these in a config screen) ---
	public static final boolean LOCK_HOSTILE_MOBS = true;
	public static final boolean LOCK_PLAYERS = true;
	public static final boolean LOCK_EXCLUDE_VILLAGERS = true;
	public static final boolean LOCK_EXCLUDE_TAMED = true;

	private KestrelSettings() {
	}
}
