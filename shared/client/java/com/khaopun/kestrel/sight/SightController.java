package com.khaopun.kestrel.sight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.core.ArrowPhysics;
import com.khaopun.kestrel.core.LockRange;
import com.khaopun.kestrel.core.ProjectileSimulator;
import com.khaopun.kestrel.core.TrajectoryResult;
import com.khaopun.kestrel.feedback.Candidate;
import com.khaopun.kestrel.feedback.FeedbackBus;
import com.khaopun.kestrel.feedback.FeedbackEvent;
import com.khaopun.kestrel.feedback.SightFrame;
import com.khaopun.kestrel.feedback.ThemeStore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
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
			emitEvents(mc);
		} finally {
			FeedbackBus.frameEnd();
		}
	}

	private static void emitEvents(Minecraft mc) {
		LocalPlayer player = mc.player;
		Level level = mc.level;
		if (player == null || level == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof BowItem)) {
			return;
		}

		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float power = ArrowPhysics.powerForTime(player.getTicksUsingItem() + partial);
		if (power < ArrowPhysics.MIN_POWER) {
			return;
		}
		boolean fullyCharged = power >= ArrowPhysics.FULL_CHARGE_POWER;

		// Launch state, as Projectile#shootFromRotation + the arrow's owner constructor build it.
		Vec3 eye = player.getEyePosition(partial);
		Vec3 start = eye.subtract(0.0D, ArrowPhysics.SPAWN_EYE_OFFSET, 0.0D);
		Vec3 look = ArrowPhysics.direction(player.getViewXRot(partial), player.getViewYRot(partial));
		Vec3 movement = player.getKnownMovement();
		Vec3 velocity = look.scale(power * ArrowPhysics.SPEED_PER_POWER)
				.add(movement.x, player.onGround() ? 0.0D : movement.y, movement.z);

		// One entity query per frame, shared by the simulator and the candidate list.
		double range = LockRange.fullCharge();
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(range),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());

		TrajectoryResult result = ProjectileSimulator.simulate(level, player, start, velocity, nearby,
				KestrelSettings.TRAJECTORY_MAX_TICKS);

		boolean rightHanded = (player.getUsedItemHand() == InteractionHand.MAIN_HAND)
				== (player.getMainArm() == HumanoidArm.RIGHT);
		float yaw = player.getViewYRot(partial) * 0.017453292F;
		Vec3 side = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw)).scale(rightHanded ? 1.0D : -1.0D);

		SightFrame frame = new SightFrame(eye, look, side, result.points(), result.impact(), fullyCharged);
		FeedbackBus.emit(FeedbackEvent.TRAJECTORY_VISIBLE, frame);
		FeedbackBus.emit(FeedbackEvent.IMPACT_POINT, frame);
		if (fullyCharged) {
			FeedbackBus.emit(FeedbackEvent.FULL_CHARGE, frame);
		}

		double rangeSqr = range * range;
		List<Entity> lockable = new ArrayList<>();
		for (Entity entity : nearby) {
			if (LockFilter.isLockable(player, entity) && entity.getBoundingBox().distanceToSqr(eye) <= rangeSqr) {
				lockable.add(entity);
			}
		}
		lockable.sort(Comparator.comparingDouble(e -> e.getBoundingBox().distanceToSqr(eye)));

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
	}
}
