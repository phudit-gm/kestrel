package com.khaopun.kestrel.feedback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The only way logic talks to feedback. Events are level-triggered: a logic frame emits every event that is
 * currently true, then calls {@link #frameEnd()}. Listeners work out enter/exit themselves.
 */
public final class FeedbackBus {
	private static final List<FeedbackListener> LISTENERS = new CopyOnWriteArrayList<>();

	private FeedbackBus() {
	}

	public static void register(FeedbackListener listener) {
		LISTENERS.add(listener);
	}

	public static void emit(FeedbackEvent event, Object payload) {
		for (FeedbackListener listener : LISTENERS) {
			listener.onEvent(event, payload);
		}
	}

	public static void frameEnd() {
		for (FeedbackListener listener : LISTENERS) {
			listener.onFrameEnd();
		}
	}
}
