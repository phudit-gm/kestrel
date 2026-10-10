package com.khaopun.kestrel.sight;

import net.minecraft.client.Minecraft;

/**
 * What the server allows. A server owner switches a feature off by creating a scoreboard team with a reserved
 * name (for example {@code /team add ks_off_modeb}); the client sees every team, so this needs no server mod, works
 * on vanilla, Paper and datapack-only servers, and the datapack in {@code datapacks/kestrel_rules} does exactly that.
 * Nothing here can stop a modified client; it is a courtesy the mod honours, not anti-cheat.
 */
public final class ServerPolicy {
	/** Switches everything off. */
	public static final String OFF_ALL = "ks_off_all";
	/** No trajectory line or impact point. */
	public static final String OFF_PATH = "ks_off_path";
	/** No target boxes and no locking; implies no prediction and no Mode B. */
	public static final String OFF_LOCK = "ks_off_lock";
	/** No predicted position (the ghost) and no lead; implies no Mode B. */
	public static final String OFF_PREDICT = "ks_off_predict";
	/** No Mode B (automatic camera aim). */
	public static final String OFF_MODE_B = "ks_off_modeb";
	/** Players cannot be targeted. */
	public static final String OFF_PLAYERS = "ks_off_players";

	private ServerPolicy() {
	}

	private static boolean has(Minecraft mc, String team) {
		return mc.level != null && mc.level.getScoreboard().getPlayerTeam(team) != null;
	}

	public static boolean allowSight(Minecraft mc) {
		return !has(mc, OFF_ALL);
	}

	public static boolean allowPath(Minecraft mc) {
		return allowSight(mc) && !has(mc, OFF_PATH);
	}

	public static boolean allowLock(Minecraft mc) {
		return allowSight(mc) && !has(mc, OFF_LOCK);
	}

	public static boolean allowPredict(Minecraft mc) {
		return allowLock(mc) && !has(mc, OFF_PREDICT);
	}

	public static boolean allowModeB(Minecraft mc) {
		return allowPredict(mc) && !has(mc, OFF_MODE_B);
	}

	public static boolean allowPlayers(Minecraft mc) {
		return allowLock(mc) && !has(mc, OFF_PLAYERS);
	}
}
