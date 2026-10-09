package com.khaopun.kestrel.feedback;

import java.util.List;

import net.minecraft.world.phys.Vec3;

/**
 * Payload of the trajectory-related events.
 *
 * @param eye          camera-side eye position
 * @param look         unit view direction
 * @param side         unit horizontal vector pointing toward the bow hand
 * @param points       simulated positions, last one = impact
 * @param impact       where the arrow would stop
 * @param fullyCharged true at full draw
 */
public record SightFrame(Vec3 eye, Vec3 look, Vec3 side, List<Vec3> points, Vec3 impact, boolean fullyCharged) {
}
