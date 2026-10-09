package com.khaopun.kestrel.feedback;

import net.minecraft.world.phys.AABB;

/** Payload of the entity events: a lockable entity and its frame-interpolated bounding box. */
public record Candidate(int id, AABB box) {
}
