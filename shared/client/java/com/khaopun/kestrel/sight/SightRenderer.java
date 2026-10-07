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
import net.minecraft.world.entity.Entity;
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

		drawTrajectory(result, fullyCharged);
		drawLockBoxes(player, eye, range, nearby, result.hitEntity(), partial);
		if (fullyCharged) {
			drawFullChargeIndicator(eye, look);
		}
	}

	private static void drawTrajectory(TrajectoryResult result, boolean fullyCharged) {
		int color = fullyCharged ? KestrelSettings.TRAJECTORY_COLOR_FULL_CHARGE : KestrelSettings.TRAJECTORY_COLOR;
		List<Vec3> points = result.points();
		for (int i = 1; i < points.size(); i++) {
			Gizmos.line(points.get(i - 1), points.get(i), color, KestrelSettings.TRAJECTORY_LINE_WIDTH);
		}
		double h = KestrelSettings.IMPACT_MARKER_HALF_SIZE;
		Vec3 impact = result.impact();
		Gizmos.cuboid(new AABB(impact.x - h, impact.y - h, impact.z - h, impact.x + h, impact.y + h, impact.z + h),
				GizmoStyle.stroke(KestrelSettings.IMPACT_COLOR, KestrelSettings.IMPACT_MARKER_LINE_WIDTH));
	}

	private static void drawLockBoxes(LocalPlayer player, Vec3 eye, double range, List<Entity> nearby, Entity target,
			float partial) {
		double rangeSqr = range * range;
		List<Entity> lockable = new ArrayList<>();
		for (Entity entity : nearby) {
			if (LockFilter.isLockable(player, entity) && entity.getBoundingBox().distanceToSqr(eye) <= rangeSqr) {
				lockable.add(entity);
			}
		}
		lockable.sort(Comparator.comparingDouble(e -> e.getBoundingBox().distanceToSqr(eye)));

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
				Gizmos.billboardText(KestrelSettings.TARGET_TEXT,
						new Vec3((box.minX + box.maxX) / 2.0D, box.maxY + KestrelSettings.TARGET_TEXT_HEIGHT_ABOVE_BOX,
								(box.minZ + box.maxZ) / 2.0D),
						TextGizmo.Style.forColorAndCentered(KestrelSettings.TARGET_TEXT_COLOR));
			}
		}
	}

	private static void drawFullChargeIndicator(Vec3 eye, Vec3 look) {
		Vec3 position = eye.add(look.scale(KestrelSettings.FULL_CHARGE_TEXT_DISTANCE))
				.subtract(0.0D, KestrelSettings.FULL_CHARGE_TEXT_DROP, 0.0D);
		Gizmos.billboardText(KestrelSettings.FULL_CHARGE_TEXT, position,
				TextGizmo.Style.forColorAndCentered(KestrelSettings.FULL_CHARGE_TEXT_COLOR));
	}
}
