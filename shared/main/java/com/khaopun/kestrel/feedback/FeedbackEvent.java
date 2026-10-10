package com.khaopun.kestrel.feedback;

/**
 * Everything the game logic can announce. Logic code only emits these; what is shown or played is decided by the
 * feedback layer from the active theme. Events marked "later" have no emitter yet.
 */
public enum FeedbackEvent {
	TRAJECTORY_VISIBLE,
	IMPACT_POINT,
	/** Multishot: one per side projectile, every frame. Payload {@link SightFrame}. */
	TRAJECTORY_EXTRA,
	CANDIDATE_IN_RANGE,
	TARGET_UNDER_TRAJECTORY,
	FULL_CHARGE,
	/** Every frame while a target is locked. Payload {@link LockInfo}. */
	TARGET_LOCKED,
	/** Once, when the player unlocks (key, or the held item stops being a bow). Payload {@link UnlockInfo}. */
	TARGET_UNLOCKED,
	/** Once, when the lock is lost by itself (died, out of range, no line of sight). Payload {@link UnlockInfo}. */
	TARGET_LOST,
	/** Every frame the predicted position is known (locked and drawing). Payload {@link GhostInfo}. */
	GHOST_PREDICTED,
	/** Every frame, once per unlocked candidate that is predicted. Payload {@link GhostInfo}. */
	GHOST_OTHER,
	/** Every frame the camera is still turning toward the aim point (Mode B). Payload {@link GhostInfo}. */
	AIM_TURNING,
	/** Every frame the impact point touches the ghost. Payload {@link GhostInfo}. */
	AIM_ALIGNED,
	/** Every frame the ghost is known but the impact point is not on it. Payload {@link GhostInfo}. */
	AIM_MOVING,
	/** Every frame the locked target cannot be reached. Payload {@link GhostInfo}. */
	TARGET_UNREACHABLE
}
