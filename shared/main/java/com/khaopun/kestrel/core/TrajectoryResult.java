package com.khaopun.kestrel.core;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Outcome of one projectile simulation.
 *
 * @param points    positions at the start of the flight and after every simulated tick, last one = impact
 * @param impact    where the arrow stops (block face / entity box hit, or the last point if it never hit anything)
 * @param hitEntity the entity the arrow would hit first, or null
 * @param hitBlock  true if the arrow stopped on a block
 */
public record TrajectoryResult(List<Vec3> points, Vec3 impact, @Nullable Entity hitEntity, boolean hitBlock) {
}
