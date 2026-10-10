package com.khaopun.kestrel.sight;

import java.util.List;

import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.core.AimSolver;
import com.khaopun.kestrel.core.LockRange;
import com.khaopun.kestrel.core.ShotProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Mode B: while drawing a bow with a target, the camera turns to the aim point by itself and the mouse switches
 * target instead of aiming. See {@code brief/step3-pseudocode.md}.
 */
public final class ModeB {
	/** Entity#turn multiplies the mouse value by this to get degrees. */
	private static final double TURN_SCALE = 0.15D;

	private static double accumYaw;
	private static double accumPitch;
	private static int cooldown;
	private static long lastNanos = System.nanoTime();

	private ModeB() {
	}

	/** Evaluated live, so any exit condition gives the mouse back at once. */
	public static boolean isActive(Minecraft mc) {
		LocalPlayer player = mc.player;
		return player != null && mc.level != null && mc.mouseHandler.isMouseGrabbed() && player.isAlive()
				&& HeldWeapon.current(player, 1.0F) != null && LockController.isLocked() && AimMode.modeB(mc);
	}

	/** Called by the Entity#turn mixin. Returns true if the turn must be swallowed. */
	public static boolean consumeTurn(Entity entity, double yRot, double xRot) {
		Minecraft mc = Minecraft.getInstance();
		if (entity != mc.player || !isActive(mc)) {
			return false;
		}
		accumYaw += yRot * TURN_SCALE;
		accumPitch += xRot * TURN_SCALE;
		return true;
	}

	/** Runs every client tick: handles target switching by mouse flick. */
	public static void onTick(Minecraft mc) {
		if (!isActive(mc)) {
			accumYaw = 0.0D;
			accumPitch = 0.0D;
			return;
		}
		if (cooldown > 0) {
			cooldown--;
		}
		accumYaw *= KestrelSettings.MODE_B_ACCUM_DECAY;
		accumPitch *= KestrelSettings.MODE_B_ACCUM_DECAY;
		double len = Math.hypot(accumYaw, accumPitch);
		if (cooldown > 0 || len < KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES) {
			return;
		}
		Entity next = pickInDirection(mc, accumYaw / len, accumPitch / len);
		accumYaw = 0.0D;
		accumPitch = 0.0D;
		if (next != null) {
			LockController.switchTo(next);
			cooldown = KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS;
		}
	}

	/** The candidate lying in the given screen direction (yaw right, pitch down) from the current target. */
	private static @Nullable Entity pickInDirection(Minecraft mc, double dirYaw, double dirPitch) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		Entity current = LockController.target(mc);
		if (current == null) {
			return null;
		}
		Vec3 eye = player.getEyePosition();
		float[] from = AimSolver.anglesTo(current.getBoundingBox().getCenter().subtract(eye));
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(LockRange.fullCharge()),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());
		Entity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Entity entity : LockController.lockable(player, level, eye, nearby)) {
			if (entity == current) {
				continue;
			}
			float[] to = AimSolver.anglesTo(entity.getBoundingBox().getCenter().subtract(eye));
			// AimSolver's pitch points up; the mouse's pitch points down, so flip it.
			double dy = Mth.wrapDegrees(to[0] - from[0]);
			double dp = -(to[1] - from[1]);
			double dist = Math.hypot(dy, dp);
			if (dist < 1.0E-6D || (dy * dirYaw + dp * dirPitch) / dist < 0.5D) {
				continue;
			}
			if (dist < bestDistance) {
				bestDistance = dist;
				best = entity;
			}
		}
		return best;
	}

	/**
	 * Turns the camera toward the aim point. Call once per frame.
	 *
	 * @return true while the camera is still turning (the target box flashes)
	 */
	public static boolean drive(Minecraft mc, LocalPlayer player, Vec3 start, ShotProfile shot, Vec3 aimPoint) {
		long now = System.nanoTime();
		double dt = Math.min(0.1D, (now - lastNanos) / 1.0e9D);
		lastNanos = now;
		if (!isActive(mc)) {
			return false;
		}
		Vec3 movement = player.getKnownMovement();
		Vec3 motion = shot.addsShooterMotion() ? new Vec3(movement.x, player.onGround() ? 0.0D : movement.y, movement.z) : Vec3.ZERO;
		float[] want = AimSolver.solve(start, shot.speed(), motion, aimPoint, shot.model());

		float yaw = player.getYRot();
		float pitch = player.getXRot();
		double dyaw = Mth.wrapDegrees(want[0] - yaw);
		double dpitch = Mth.clamp(want[1], -90.0F, 90.0F) - pitch;
		double remaining = Math.hypot(dyaw, dpitch);
		double step = KestrelSettings.MODE_B_TURN_DEG_PER_SEC * dt;
		double k = remaining <= step || remaining < 1.0E-9D ? 1.0D : step / remaining;
		float newYaw = (float) (yaw + dyaw * k);
		float newPitch = (float) Mth.clamp(pitch + dpitch * k, -90.0D, 90.0D);
		player.setYRot(newYaw);
		player.setXRot(newPitch);
		player.yRotO = newYaw;
		player.xRotO = newPitch;
		return remaining * (1.0D - k) > KestrelSettings.MODE_B_ALIGNED_DEGREES;
	}
}
