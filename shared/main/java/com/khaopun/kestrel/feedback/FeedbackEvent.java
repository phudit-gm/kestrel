package com.khaopun.kestrel.feedback;

/**
 * Everything the game logic can announce. Logic code only emits these; what is shown or played is decided by the
 * feedback layer from the active theme. Events marked "later" have no emitter yet.
 */
public enum FeedbackEvent {
	TRAJECTORY_VISIBLE,
	IMPACT_POINT,
	CANDIDATE_IN_RANGE,
	TARGET_UNDER_TRAJECTORY,
	FULL_CHARGE,
	/** Later (Step 2). */
	TARGET_LOCKED,
	/** Later (Step 2). */
	TARGET_UNLOCKED,
	/** Later (Step 2). */
	TARGET_LOST,
	/** Later (Step 3). */
	AIM_ALIGNED,
	/** Later (Step 3). */
	AIM_MOVING,
	/** Later (Step 2). */
	TARGET_UNREACHABLE
}
