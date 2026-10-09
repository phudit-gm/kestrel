package com.khaopun.kestrel.feedback;

import net.minecraft.world.phys.AABB;

/** The locked target: id and its frame-interpolated bounding box. */
public record LockInfo(int id, AABB box) {
}
