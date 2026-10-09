package com.khaopun.kestrel.feedback;

import com.khaopun.kestrel.core.Prediction;

import net.minecraft.world.phys.AABB;

/**
 * Where the locked target is predicted to be.
 *
 * @param id      the locked target
 * @param ghost   the target's bounding box moved to the predicted position
 * @param kind    clear, faded (erratic target) or unreachable
 * @param aligned true if the arrow trajectory touches the ghost
 */
public record GhostInfo(int id, AABB ghost, Prediction.Kind kind, boolean aligned) {
}
