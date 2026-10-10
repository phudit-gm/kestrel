package com.khaopun.kestrel.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import org.jspecify.annotations.Nullable;

/**
 * Tick-by-tick arrow simulator that mirrors AbstractArrow#tick.
 *
 * <pre>
 * Pseudocode (one arrow tick, as in the game):
 *
 *   state: pos, vel
 *   repeat up to maxTicks:
 *     if arrow is in water at the start of the tick:      vel = vel * WATER_DRAG
 *     blockHit = clip(pos -> pos + vel, COLLIDER blocks, fluids ignored)
 *     end      = blockHit.location if it hit something, else pos + vel
 *     entityHit = nearest candidate entity whose box (inflated by ENTITY_HIT_MARGIN)
 *                 is crossed by the segment pos -> end
 *     if entityHit:  record impact at its entry point, stop
 *     if blockHit:   record impact at blockHit.location, stop
 *     pos = pos + vel
 *     if arrow is not in water after moving:              vel = vel * AIR_DRAG
 *     vel.y = vel.y - GRAVITY
 *     if pos fell below the world: stop
 * </pre>
 */
public final class ProjectileSimulator {
	private ProjectileSimulator() {
	}

	/**
	 * @param shooter    used for the collision context and never counted as a hit target
	 * @param candidates entities the arrow may hit (the caller fetches them once per frame)
	 */
	public static TrajectoryResult simulate(Level level, @Nullable Entity shooter, Vec3 start, Vec3 startVelocity,
			List<Entity> candidates, int maxTicks) {
		return simulate(level, shooter, start, startVelocity, candidates, maxTicks, FlightModel.ARROW);
	}

	/** Same, for any projectile: gravity and drag come from {@code model}. */
	public static TrajectoryResult simulate(Level level, @Nullable Entity shooter, Vec3 start, Vec3 startVelocity,
			List<Entity> candidates, int maxTicks, FlightModel model) {
		List<Vec3> points = new ArrayList<>(maxTicks + 1);
		Vec3 pos = start;
		Vec3 vel = startVelocity;
		points.add(pos);
		int minY = level.getMinY() - 16;
		CollisionContext context = shooter == null ? CollisionContext.empty() : CollisionContext.of(shooter);

		for (int tick = 0; tick < maxTicks; tick++) {
			if (isInWater(level, pos)) {
				vel = vel.scale(model.waterDrag());
			}

			Vec3 next = pos.add(vel);
			BlockHitResult blockHit = level.clip(
					new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
			boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
			Vec3 end = hitBlock ? blockHit.getLocation() : next;

			Entity hitEntity = null;
			Vec3 entityPoint = null;
			double best = Double.MAX_VALUE;
			for (Entity candidate : candidates) {
				if (candidate == shooter) {
					continue;
				}
				AABB box = candidate.getBoundingBox().inflate(ArrowPhysics.ENTITY_HIT_MARGIN);
				Vec3 hit = box.clip(pos, end).orElse(null);
				if (hit == null && box.contains(pos)) {
					hit = pos;
				}
				if (hit != null) {
					double d = pos.distanceToSqr(hit);
					if (d < best) {
						best = d;
						hitEntity = candidate;
						entityPoint = hit;
					}
				}
			}

			if (hitEntity != null) {
				points.add(entityPoint);
				return new TrajectoryResult(points, entityPoint, hitEntity, false);
			}
			if (hitBlock) {
				points.add(end);
				return new TrajectoryResult(points, end, null, true);
			}

			pos = next;
			points.add(pos);
			if (!isInWater(level, pos)) {
				vel = vel.scale(model.airDrag());
			}
			vel = vel.add(0.0D, -model.gravity(), 0.0D);

			if (pos.y < minY) {
				break;
			}
		}
		return new TrajectoryResult(points, pos, null, false);
	}

	/** Approximation of Entity#isInWater for a point-sized arrow: the point is inside a water fluid's surface. */
	private static boolean isInWater(Level level, Vec3 pos) {
		BlockPos blockPos = BlockPos.containing(pos);
		FluidState fluid = level.getFluidState(blockPos);
		return fluid.is(FluidTags.WATER) && pos.y <= blockPos.getY() + fluid.getHeight(level, blockPos);
	}
}
