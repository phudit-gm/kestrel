package com.khaopun.kestrel.sight;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.khaopun.kestrel.core.LockRange;
import com.khaopun.kestrel.core.TargetMotion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * Keeps a movement history for every lockable entity in range while a bow is held, so each of them can be
 * predicted and not only the locked one. Runs every client tick; does nothing (and forgets everything) when no
 * bow is held, so it costs nothing outside of archery.
 */
public final class TargetTracker {
	private static final Map<Integer, TargetMotion> MOTIONS = new HashMap<>();

	private TargetTracker() {
	}

	/** Movement history of an entity, or null if it is not being tracked. */
	public static @Nullable TargetMotion motion(int id) {
		return MOTIONS.get(id);
	}

	public static void clear() {
		MOTIONS.clear();
	}

	public static void onTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null || !HeldWeapon.holdingAny(player)) {
			MOTIONS.clear();
			return;
		}
		double range = LockRange.fullCharge();
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(range),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());
		List<Entity> lockable = LockController.lockable(player, level, player.getEyePosition(), nearby);

		Set<Integer> seen = new HashSet<>();
		for (Entity entity : lockable) {
			seen.add(entity.getId());
			MOTIONS.computeIfAbsent(entity.getId(), id -> new TargetMotion()).record(entity.position());
		}
		MOTIONS.keySet().retainAll(seen);
	}
}
