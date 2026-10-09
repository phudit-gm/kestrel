package com.khaopun.kestrel.sight;

import java.util.ArrayList;
import java.util.List;

import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.core.LockRange;
import com.khaopun.kestrel.core.TargetMotion;
import com.khaopun.kestrel.feedback.FeedbackBus;
import com.khaopun.kestrel.feedback.FeedbackEvent;
import com.khaopun.kestrel.feedback.LockInfo;
import com.khaopun.kestrel.feedback.UnlockInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The target lock: what is locked, when it is released, and the locked target's recent movement.
 *
 * <p>Pressing the lock key ({@link #onKey}) locks the candidate nearest the crosshair, switches to a different
 * candidate the crosshair is clearly on, or unlocks. {@link #onTick} runs every client tick: it records the
 * target's movement and releases the lock when the target dies or is removed, leaves range, is out of sight for
 * too long, or when no bow is held. Releases are announced as feedback events; nothing here decides how they look.
 */
public final class LockController {
	private static int lockedId = -1;
	private static final TargetMotion MOTION = new TargetMotion();
	private static int noSightTicks;
	private static AABB lastBox;
	private static final List<Object[]> PENDING = new ArrayList<>();

	private LockController() {
	}

	public static boolean isLocked() {
		return lockedId >= 0;
	}

	public static @Nullable Entity target(Minecraft mc) {
		return mc.level == null || lockedId < 0 ? null : mc.level.getEntity(lockedId);
	}

	public static TargetMotion motion() {
		return MOTION;
	}

	/** Lockable entities within range of the player, nearest first. */
	public static List<Entity> lockable(LocalPlayer player, Level level, Vec3 eye, List<Entity> nearby) {
		double rangeSqr = LockRange.fullCharge() * LockRange.fullCharge();
		List<Entity> out = new ArrayList<>();
		for (Entity entity : nearby) {
			if (LockFilter.isLockable(player, entity) && entity.getBoundingBox().distanceToSqr(eye) <= rangeSqr) {
				out.add(entity);
			}
		}
		out.sort(java.util.Comparator.comparingDouble(e -> e.getBoundingBox().distanceToSqr(eye)));
		return out;
	}

	public static void onKey(Minecraft mc) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 eye = player.getEyePosition(partial);
		Vec3 look = player.getViewVector(partial);
		double range = LockRange.fullCharge();
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(range),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());
		List<Entity> candidates = lockable(player, level, eye, nearby);

		Entity best = null;
		double bestAngle = Double.MAX_VALUE;
		for (Entity entity : candidates) {
			double angle = angleDegrees(look, entity.getBoundingBox().getCenter().subtract(eye));
			if (angle < bestAngle) {
				bestAngle = angle;
				best = entity;
			}
		}

		if (!isLocked()) {
			if (best != null && bestAngle <= KestrelSettings.LOCK_MAX_ANGLE_DEGREES) {
				lock(best);
			}
			return;
		}
		if (best != null && best.getId() != lockedId && bestAngle <= KestrelSettings.LOCK_SWITCH_ANGLE_DEGREES) {
			release(false); // switching: no "unlocked" announcement for the old target
			lock(best);
			return;
		}
		release(true);
		announceUnlock(FeedbackEvent.TARGET_UNLOCKED);
	}

	/** Runs every client tick. */
	public static void onTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null) {
			forget();
			return;
		}
		if (!isLocked()) {
			return;
		}
		Entity target = level.getEntity(lockedId);
		if (target == null || !target.isAlive() || target.isRemoved()) {
			release(true);
			announceUnlock(FeedbackEvent.TARGET_LOST);
			return;
		}
		lastBox = target.getBoundingBox();
		MOTION.record(target.position());

		Vec3 eye = player.getEyePosition();
		if (target.getBoundingBox().distanceToSqr(eye) > LockRange.fullCharge() * LockRange.fullCharge()) {
			release(true);
			announceUnlock(FeedbackEvent.TARGET_LOST);
			return;
		}
		if (player.hasLineOfSight(target)) {
			noSightTicks = 0;
		} else if (++noSightTicks > KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS) {
			release(true);
			announceUnlock(FeedbackEvent.TARGET_LOST);
			return;
		}
		if (!(player.getMainHandItem().getItem() instanceof BowItem) && !(player.getOffhandItem().getItem() instanceof BowItem)) {
			release(true);
			announceUnlock(FeedbackEvent.TARGET_UNLOCKED);
		}
	}

	/** Emits this frame's lock events. Call once per rendered frame, before the bow-sight events. */
	public static void emitFrame(Minecraft mc, float partial) {
		for (Object[] pending : PENDING) {
			FeedbackBus.emit((FeedbackEvent) pending[0], pending[1]);
		}
		PENDING.clear();
		Entity target = target(mc);
		if (target != null) {
			Vec3 offset = target.getPosition(partial).subtract(target.position());
			FeedbackBus.emit(FeedbackEvent.TARGET_LOCKED, new LockInfo(lockedId, target.getBoundingBox().move(offset)));
		}
	}

	public static void forget() {
		lockedId = -1;
		MOTION.clear();
		noSightTicks = 0;
		PENDING.clear();
	}

	private static void lock(Entity entity) {
		lockedId = entity.getId();
		MOTION.clear();
		MOTION.record(entity.position());
		noSightTicks = 0;
		lastBox = entity.getBoundingBox();
	}

	/** Clears the lock; remembers the id and box so an announcement can still be made. */
	private static void release(boolean keepForAnnouncement) {
		if (keepForAnnouncement) {
			announcedId = lockedId;
		}
		lockedId = -1;
		MOTION.clear();
		noSightTicks = 0;
	}

	private static int announcedId = -1;

	private static void announceUnlock(FeedbackEvent event) {
		if (announcedId >= 0 && lastBox != null) {
			PENDING.add(new Object[] {event, new UnlockInfo(announcedId, lastBox)});
		}
		announcedId = -1;
	}

	private static double angleDegrees(Vec3 look, Vec3 to) {
		double len = to.length();
		if (len < 1.0E-6D) {
			return 0.0D;
		}
		double cos = Math.max(-1.0D, Math.min(1.0D, look.dot(to) / len));
		return Math.toDegrees(Math.acos(cos));
	}
}
