package com.khaopun.kestrel.sight;

import java.util.List;

import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.core.ArrowPhysics;
import com.khaopun.kestrel.core.LockRange;
import com.khaopun.kestrel.core.Prediction;
import com.khaopun.kestrel.core.Predictor;
import com.khaopun.kestrel.core.ProjectileSimulator;
import com.khaopun.kestrel.core.ShotProfile;
import com.khaopun.kestrel.core.TargetMotion;
import com.khaopun.kestrel.core.TrajectoryResult;
import com.khaopun.kestrel.feedback.Candidate;
import com.khaopun.kestrel.feedback.FeedbackBus;
import com.khaopun.kestrel.feedback.FeedbackEvent;
import com.khaopun.kestrel.feedback.GhostInfo;
import com.khaopun.kestrel.feedback.SightFrame;
import com.khaopun.kestrel.feedback.ThemeStore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bow sight logic: simulates the arrow, picks lockable entities, and announces what is true this frame as
 * {@link FeedbackEvent}s. It never decides how anything looks or sounds; that is the feedback layer's job.
 *
 * <p>Call {@link #update} once per rendered frame from a world-render event.
 */
public final class SightController {
	private SightController() {
	}

	public static void update(Minecraft mc) {
		try {
			LockController.emitFrame(mc, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			emitEvents(mc);
		} finally {
			FeedbackBus.frameEnd();
		}
	}

	private static void emitEvents(Minecraft mc) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null) {
			return;
		}

		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		ShotProfile shot = HeldWeapon.current(player, partial);
		if (shot == null) {
			return;
		}
		boolean fullyCharged = shot.fullyCharged();

		// Launch state, as Projectile#shootFromRotation (bow, trident) or Projectile#shoot (crossbow) builds it.
		Vec3 eye = player.getEyePosition(partial);
		Vec3 start = eye.subtract(0.0D, shot.model().spawnDrop(), 0.0D);
		Vec3 look = ArrowPhysics.direction(player.getViewXRot(partial), player.getViewYRot(partial));
		Vec3 velocity = launchVelocity(player, shot, look);

		// One entity query per frame, shared by the simulator and the candidate list.
		double range = LockRange.fullCharge();
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(range),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());

		TrajectoryResult result = ProjectileSimulator.simulate(level, player, start, velocity, nearby,
				shot.maxTicks(), shot.model());

		boolean rightHanded = (player.getUsedItemHand() == InteractionHand.MAIN_HAND)
				== (player.getMainArm() == HumanoidArm.RIGHT);
		float yaw = player.getViewYRot(partial) * 0.017453292F;
		Vec3 side = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw)).scale(rightHanded ? 1.0D : -1.0D);

		SightFrame frame = new SightFrame(eye, look, side, result.points(), result.impact(), fullyCharged);
		FeedbackBus.emit(FeedbackEvent.TRAJECTORY_VISIBLE, frame);
		FeedbackBus.emit(FeedbackEvent.IMPACT_POINT, frame);
		// Multishot: the side projectiles get their own lines. Lock, ghost and Mode B use the centre one only.
		for (double angle : shot.extraAngles()) {
			Vec3 sideLook = rotateAroundUp(player, look, angle);
			TrajectoryResult sideResult = ProjectileSimulator.simulate(level, player, start,
					launchVelocity(player, shot, sideLook), nearby, shot.maxTicks(), shot.model());
			FeedbackBus.emit(FeedbackEvent.TRAJECTORY_EXTRA, new SightFrame(eye, sideLook, side, sideResult.points(),
					sideResult.impact(), fullyCharged));
		}
		if (fullyCharged) {
			FeedbackBus.emit(FeedbackEvent.FULL_CHARGE, frame);
		}

		List<Entity> lockable = LockController.lockable(player, level, eye, nearby);

		int count = Math.min(lockable.size(), ThemeStore.theme().integer("limits.maxCandidates"));
		for (int i = 0; i < count; i++) {
			Entity entity = lockable.get(i);
			// Interpolate so the box follows the entity smoothly between ticks.
			Vec3 offset = entity.getPosition(partial).subtract(entity.position());
			Candidate candidate = new Candidate(entity.getId(), entity.getBoundingBox().move(offset));
			FeedbackBus.emit(FeedbackEvent.CANDIDATE_IN_RANGE, candidate);
			if (entity == result.hitEntity()) {
				FeedbackBus.emit(FeedbackEvent.TARGET_UNDER_TRAJECTORY, candidate);
			}
		}

		emitPrediction(mc, player, start, shot, result);
		emitOthers(mc, player, start, shot, lockable, partial);
	}

	/** Launch velocity of one projectile: aim direction times speed, plus the shooter's motion where the game adds it. */
	private static Vec3 launchVelocity(LocalPlayer player, ShotProfile shot, Vec3 direction) {
		Vec3 velocity = direction.scale(shot.speed());
		if (shot.addsShooterMotion()) {
			Vec3 movement = player.getKnownMovement();
			velocity = velocity.add(movement.x, player.onGround() ? 0.0D : movement.y, movement.z);
		}
		return velocity;
	}

	/** Rotates a direction around the player's up axis, as CrossbowItem does for Multishot (Rodrigues' formula). */
	private static Vec3 rotateAroundUp(LocalPlayer player, Vec3 direction, double degrees) {
		Vec3 axis = player.getUpVector(1.0F).normalize();
		double rad = Math.toRadians(degrees);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		return direction.scale(cos).add(axis.cross(direction).scale(sin)).add(axis.scale(axis.dot(direction) * (1.0D - cos)));
	}

	/** Predicts the nearest unlocked candidates too, so the player can see where each one is heading. */
	private static void emitOthers(Minecraft mc, LocalPlayer player, Vec3 start, ShotProfile shot, List<Entity> lockable, float partial) {
		int budget = KestrelSettings.PREDICT_MAX_OTHERS;
		double ping = pingMillis(mc, player);
		Entity locked = LockController.target(mc);
		for (Entity entity : lockable) {
			if (budget <= 0) {
				break;
			}
			if (entity == locked) {
				continue;
			}
			TargetMotion motion = TargetTracker.motion(entity.getId());
			if (motion == null || motion.isEmpty()) {
				continue;
			}
			AABB box = entity.getBoundingBox();
			Prediction prediction = Predictor.predict(motion, start, shot.speed(), shot.model(), shot.maxTicks(), ping,
					(box.maxY - box.minY) / 2.0D);
			if (prediction.kind() == Prediction.Kind.UNREACHABLE) {
				continue;
			}
			Vec3 offset = entity.getPosition(partial).subtract(entity.position());
			AABB ghost = box.move(offset).move(prediction.aim().subtract(entity.position()));
			FeedbackBus.emit(FeedbackEvent.GHOST_OTHER, new GhostInfo(entity.getId(), ghost, prediction.kind(), false));
			budget--;
		}
	}

	/** Predicts where the locked target will be and announces whether the arrow's path touches that spot. */
	private static void emitPrediction(Minecraft mc, LocalPlayer player, Vec3 start, ShotProfile shot, TrajectoryResult result) {
		Entity target = LockController.target(mc);
		if (target == null || LockController.motion().isEmpty()) {
			return;
		}
		AABB box = target.getBoundingBox();
		double bodyHeight = (box.maxY - box.minY) / 2.0D;
		Prediction prediction = Predictor.predict(LockController.motion(), start, shot.speed(), shot.model(), shot.maxTicks(),
				pingMillis(mc, player), bodyHeight);
		// The box is where the target is now, moved by the difference to the predicted position.
		Vec3 shift = prediction.aim().subtract(target.position());
		AABB ghost = box.move(shift);
		DebugShots.recordPrediction(player, target, prediction);

		boolean turning = prediction.kind() != Prediction.Kind.UNREACHABLE
				&& ModeB.drive(mc, player, start, shot, ghost.getCenter());

		boolean aligned = prediction.kind() != Prediction.Kind.UNREACHABLE && pathTouches(result.points(), ghost);
		GhostInfo info = new GhostInfo(target.getId(), ghost, prediction.kind(), aligned);
		FeedbackBus.emit(FeedbackEvent.GHOST_PREDICTED, info);
		if (turning) {
			FeedbackBus.emit(FeedbackEvent.AIM_TURNING, info);
		}
		if (prediction.kind() == Prediction.Kind.UNREACHABLE) {
			FeedbackBus.emit(FeedbackEvent.TARGET_UNREACHABLE, info);
		}
		FeedbackBus.emit(aligned ? FeedbackEvent.AIM_ALIGNED : FeedbackEvent.AIM_MOVING, info);
	}

	/** True if any step of the simulated path passes through the ghost box (grown by the arrow's hit margin). */
	private static boolean pathTouches(List<Vec3> points, AABB ghost) {
		AABB grown = ghost.inflate(ArrowPhysics.ENTITY_HIT_MARGIN);
		for (int i = 1; i < points.size(); i++) {
			if (grown.contains(points.get(i)) || grown.clip(points.get(i - 1), points.get(i)).isPresent()) {
				return true;
			}
		}
		return false;
	}

	private static double pingMillis(Minecraft mc, LocalPlayer player) {
		if (mc.getConnection() == null) {
			return 0.0D;
		}
		PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
		return info == null ? 0.0D : info.getLatency();
	}
}
