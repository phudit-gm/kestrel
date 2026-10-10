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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * The main target: chosen automatically while a bow is drawn, never by a key.
 *
 * <p>When a draw starts the candidate nearest the crosshair becomes the target. In Mode A the target then follows
 * the crosshair (it switches when the crosshair is clearly on another candidate); in Mode B it only changes through
 * {@link ModeB}. It is released silently when the draw ends, and with a "lost" announcement when the target dies,
 * leaves range or stays out of sight for too long. Nothing here decides how anything looks.
 */
public final class LockController {
	private static final TargetMotion NO_MOTION = new TargetMotion();

	private static int lockedId = -1;
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

	/** Recorded movement of the target; empty if there is no target. */
	public static TargetMotion motion() {
		TargetMotion motion = lockedId < 0 ? null : TargetTracker.motion(lockedId);
		return motion == null ? NO_MOTION : motion;
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

	/** Queues a one-off event to be announced with the next frame. */
	public static void announce(FeedbackEvent event) {
		PENDING.add(new Object[] {event, null});
	}

	/** Used by Mode B when the mouse flicks toward another candidate. */
	public static void switchTo(Entity entity) {
		lock(entity);
	}

	/** Runs every client tick. */
	public static void onTick(Minecraft mc) {
		TargetTracker.onTick(mc);
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null) {
			forget();
			return;
		}
		boolean drawing = HeldWeapon.current(player, 1.0F) != null;
		if (!drawing) {
			if (isLocked()) {
				lockedId = -1;
				noSightTicks = 0;
			}
			return;
		}

		if (isLocked()) {
			Entity target = level.getEntity(lockedId);
			if (target == null || !target.isAlive() || target.isRemoved()) {
				lose();
			} else {
				lastBox = target.getBoundingBox();
				if (target.getBoundingBox().distanceToSqr(player.getEyePosition()) > LockRange.fullCharge() * LockRange.fullCharge()) {
					lose();
				} else if (player.hasLineOfSight(target)) {
					noSightTicks = 0;
				} else if (++noSightTicks > KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS) {
					lose();
				}
			}
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(LockRange.fullCharge()),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());
		Entity best = null;
		double bestAngle = Double.MAX_VALUE;
		for (Entity entity : lockable(player, level, eye, nearby)) {
			double angle = angleDegrees(look, entity.getBoundingBox().getCenter().subtract(eye));
			if (angle < bestAngle) {
				bestAngle = angle;
				best = entity;
			}
		}
		if (best == null) {
			return;
		}
		if (!isLocked()) {
			if (bestAngle <= KestrelSettings.LOCK_MAX_ANGLE_DEGREES) {
				lock(best);
			}
		} else if (!AimMode.modeB(mc) && best.getId() != lockedId && bestAngle <= KestrelSettings.LOCK_SWITCH_ANGLE_DEGREES) {
			lock(best);
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
		noSightTicks = 0;
		PENDING.clear();
		TargetTracker.clear();
	}

	private static void lock(Entity entity) {
		lockedId = entity.getId();
		noSightTicks = 0;
		lastBox = entity.getBoundingBox();
	}

	/** Releases the target and announces that it was lost. */
	private static void lose() {
		if (lockedId >= 0 && lastBox != null) {
			PENDING.add(new Object[] {FeedbackEvent.TARGET_LOST, new UnlockInfo(lockedId, lastBox)});
		}
		lockedId = -1;
		noSightTicks = 0;
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
