package com.khaopun.kestrel.feedback;

public interface FeedbackListener {
	/** Called once per frame for every event that is active in that frame. */
	void onEvent(FeedbackEvent event, Object payload);

	/** Called after the last event of a frame (also when no event was emitted, so effects can fade out). */
	void onFrameEnd();
}
