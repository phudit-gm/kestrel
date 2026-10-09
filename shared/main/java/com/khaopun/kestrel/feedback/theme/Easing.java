package com.khaopun.kestrel.feedback.theme;

/** Named easing curves a theme can pick. Input and output are in 0..1 (outBack overshoots above 1). */
public final class Easing {
	private Easing() {
	}

	public static double apply(String name, double t) {
		double x = Math.max(0.0D, Math.min(1.0D, t));
		return switch (name) {
			case "linear" -> x;
			case "outQuad" -> 1.0D - (1.0D - x) * (1.0D - x);
			case "inOutCubic" -> x < 0.5D ? 4.0D * x * x * x : 1.0D - Math.pow(-2.0D * x + 2.0D, 3.0D) / 2.0D;
			case "outBack" -> {
				double c1 = 1.70158D;
				double c3 = c1 + 1.0D;
				yield 1.0D + c3 * Math.pow(x - 1.0D, 3.0D) + c1 * Math.pow(x - 1.0D, 2.0D);
			}
			default -> 1.0D - Math.pow(1.0D - x, 3.0D); // outCubic
		};
	}
}
