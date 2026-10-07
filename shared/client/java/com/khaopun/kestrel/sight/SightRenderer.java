package com.khaopun.kestrel.sight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.core.ArrowPhysics;
import com.khaopun.kestrel.core.LockRange;
import com.khaopun.kestrel.core.ProjectileSimulator;
import com.khaopun.kestrel.core.TrajectoryResult;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the bow sight while a bow is being drawn: predicted trajectory, impact marker, boxes around lockable
 * entities in range, the TARGET highlight, and the full-charge indicator.
 *
 * <p>Everything is drawn through the game's own gizmo system, so the same code works on both supported versions.
 * Call {@link #render} from a world-render event that fires while the per-frame gizmo collector is active.
 */
public final class SightRenderer {
	private SightRenderer() {
	}

	public static void render(Minecraft mc) {
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

		// One entity query per frame, shared by the simulator and the box drawing.
		double range = LockRange.fullCharge();
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().inflate(range),
				e -> e.isAlive() && e.isPickable() && !e.isSpectator());

		TrajectoryResult result = ProjectileSimulator.simulate(level, player, start, velocity, nearby,
				KestrelSettings.TRAJECTORY_MAX_TICKS);

		boolean rightHanded = (player.getUsedItemHand() == InteractionHand.MAIN_HAND)
				== (player.getMainArm() == HumanoidArm.RIGHT);
		float yaw = player.getViewYRot(partial) * 0.017453292F;
		Vec3 side = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw)).scale(rightHanded ? 1.0D : -1.0D);
		drawTrajectory(result, fullyCharged, side);
		boolean targetLocked = drawLockBoxes(player, eye, range, nearby, result.hitEntity(), partial);
		if (fullyCharged) {
			drawCrosshairLabel(eye, look, KestrelSettings.FULL_CHARGE_TEXT, KestrelSettings.FULL_CHARGE_TEXT_COLOR, 0);
		}
		// Second TARGET label near the crosshair: the one above the entity's head is easy to lose off-screen.
		if (targetLocked) {
			drawCrosshairLabel(eye, look, KestrelSettings.TARGET_TEXT, KestrelSettings.TARGET_TEXT_COLOR, 1);
		}
	}

	private static void drawTrajectory(TrajectoryResult result, boolean fullyCharged, Vec3 side) {
		int color = fullyCharged ? KestrelSettings.TRAJECTORY_COLOR_FULL_CHARGE : KestrelSettings.TRAJECTORY_COLOR;
		List<Vec3> points = result.points();
		// Visual only: shift the first part of the line toward the bow hand. Physics and impact are untouched.
		Vec3 previous = handShifted(points.get(0), 0, side);
		for (int i = 1; i < points.size(); i++) {
			boolean last = i == points.size() - 1;
			Vec3 current = last ? points.get(i) : handShifted(points.get(i), i, side);
			Gizmos.line(previous, current, color, KestrelSettings.TRAJECTORY_LINE_WIDTH);
			previous = current;
		}
		double h = KestrelSettings.IMPACT_MARKER_HALF_SIZE;
		Vec3 impact = result.impact();
		Gizmos.cuboid(new AABB(impact.x - h, impact.y - h, impact.z - h, impact.x + h, impact.y + h, impact.z + h),
				GizmoStyle.stroke(KestrelSettings.IMPACT_COLOR, KestrelSettings.IMPACT_MARKER_LINE_WIDTH));
	}

	private static Vec3 handShifted(Vec3 point, int index, Vec3 side) {
		double weight = 1.0D - (double) index / KestrelSettings.HAND_OFFSET_FADE_TICKS;
		if (weight <= 0.0D) {
			return point;
		}
		return point.add(side.scale(KestrelSettings.HAND_OFFSET_SIDE * weight))
				.subtract(0.0D, KestrelSettings.HAND_OFFSET_DOWN * weight, 0.0D);
	}

	private static boolean drawLockBoxes(LocalPlayer player, Vec3 eye, double range, List<Entity> nearby, Entity target,
			float partial) {
		double rangeSqr = range * range;
		List<Entity> lockable = new ArrayList<>();
		for (Entity entity : nearby) {
			if (LockFilter.isLockable(player, entity) && entity.getBoundingBox().distanceToSqr(eye) <= rangeSqr) {
				lockable.add(entity);
			}
		}
		lockable.sort(Comparator.comparingDouble(e -> e.getBoundingBox().distanceToSqr(eye)));

		boolean targetDrawn = false;
		int count = Math.min(lockable.size(), KestrelSettings.MAX_LOCK_BOXES);
		for (int i = 0; i < count; i++) {
			Entity entity = lockable.get(i);
			boolean isTarget = entity == target;
			// Interpolate so the box follows the entity smoothly between ticks.
			Vec3 offset = entity.getPosition(partial).subtract(entity.position());
			AABB box = entity.getBoundingBox().move(offset).inflate(KestrelSettings.LOCK_BOX_INFLATE);
			int color = isTarget ? KestrelSettings.TARGET_BOX_COLOR : KestrelSettings.LOCK_BOX_COLOR;
			Gizmos.cuboid(box, GizmoStyle.stroke(color, KestrelSettings.LOCK_BOX_LINE_WIDTH));
			if (isTarget) {
				targetDrawn = true;
				Gizmos.billboardText(KestrelSettings.TARGET_TEXT,
						new Vec3((box.minX + box.maxX) / 2.0D, box.maxY + KestrelSettings.TARGET_TEXT_HEIGHT_ABOVE_BOX,
								(box.minZ + box.maxZ) / 2.0D),
						TextGizmo.Style.forColorAndCentered(KestrelSettings.TARGET_TEXT_COLOR));
			}
		}
		return targetDrawn;
	}

	/** Floats a label in front of the camera, just below the crosshair; row 0 is the first line, 1 the next. */
	private static void drawCrosshairLabel(Vec3 eye, Vec3 look, String text, int color, int row) {
		Vec3 position = eye.add(look.scale(KestrelSettings.FULL_CHARGE_TEXT_DISTANCE))
				.subtract(0.0D, KestrelSettings.FULL_CHARGE_TEXT_DROP + row * KestrelSettings.CROSSHAIR_LABEL_ROW_GAP, 0.0D);
		Gizmos.billboardText(text, position, TextGizmo.Style.forColorAndCentered(color));
	}
}
