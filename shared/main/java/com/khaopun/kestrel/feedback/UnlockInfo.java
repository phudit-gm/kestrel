package com.khaopun.kestrel.feedback;

import net.minecraft.world.phys.AABB;

/** A lock that just ended: the target's id and its last known bounding box. */
public record UnlockInfo(int id, AABB box) {
}
