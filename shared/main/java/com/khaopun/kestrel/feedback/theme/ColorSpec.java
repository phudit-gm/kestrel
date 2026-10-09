package com.khaopun.kestrel.feedback.theme;

import java.awt.Color;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A theme colour. In JSON it is one of:
 * <ul>
 * <li>{@code "#RRGGBB"} or {@code "#RRGGBBAA"}: solid colour</li>
 * <li>{@code {"mode":"gradient","stops":["#..","#.."],"flow":0.0}}: colours along the element (t = 0..1); {@code flow}
 * is how fast the gradient scrolls, in cycles per second</li>
 * <li>{@code {"mode":"rainbow","speed":0.4,"span":1.0,"saturation":1.0,"brightness":1.0,"alpha":0.7}}: hue cycles
 * with time ({@code speed}, cycles per second) and along the element ({@code span}, hue range)</li>
 * </ul>
 * Colours are ARGB ints.
 */
public final class ColorSpec {
	private enum Mode {
		SOLID, GRADIENT, RAINBOW
	}

	private final Mode mode;
	private final int[] stops;
	private final double flow;
	private final double span;
	private final float saturation;
	private final float brightness;
	private final int rainbowAlpha;

	private ColorSpec(Mode mode, int[] stops, double flow, double span, float saturation, float brightness, int rainbowAlpha) {
		this.mode = mode;
		this.stops = stops;
		this.flow = flow;
		this.span = span;
		this.saturation = saturation;
		this.brightness = brightness;
		this.rainbowAlpha = rainbowAlpha;
	}

	public static ColorSpec solid(int argb) {
		return new ColorSpec(Mode.SOLID, new int[] {argb}, 0.0D, 0.0D, 0.0F, 0.0F, 0);
	}

	/** @throws IllegalArgumentException if the element is not a valid colour description */
	public static ColorSpec parse(JsonElement element) {
		if (element.isJsonPrimitive()) {
			return solid(parseHex(element.getAsString()));
		}
		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("colour must be a string or an object");
		}
		JsonObject o = element.getAsJsonObject();
		String mode = o.has("mode") ? o.get("mode").getAsString() : "";
		switch (mode) {
			case "gradient" -> {
				JsonArray array = o.getAsJsonArray("stops");
				if (array == null || array.isEmpty()) {
					throw new IllegalArgumentException("gradient needs stops");
				}
				int[] stops = new int[array.size()];
				for (int i = 0; i < stops.length; i++) {
					stops[i] = parseHex(array.get(i).getAsString());
				}
				return new ColorSpec(Mode.GRADIENT, stops, o.has("flow") ? o.get("flow").getAsDouble() : 0.0D, 0.0D, 0.0F, 0.0F, 0);
			}
			case "rainbow" -> {
				return new ColorSpec(Mode.RAINBOW, new int[0],
						o.has("speed") ? o.get("speed").getAsDouble() : 0.0D,
						o.has("span") ? o.get("span").getAsDouble() : 1.0D,
						o.has("saturation") ? o.get("saturation").getAsFloat() : 1.0F,
						o.has("brightness") ? o.get("brightness").getAsFloat() : 1.0F,
						Math.round(255.0F * (o.has("alpha") ? o.get("alpha").getAsFloat() : 1.0F)));
			}
			default -> throw new IllegalArgumentException("unknown colour mode '" + mode + "'");
		}
	}

	/**
	 * @param t       position along the element, 0..1
	 * @param seconds time in seconds, for flowing gradients and rainbows
	 */
	public int at(double t, double seconds) {
		switch (mode) {
			case GRADIENT -> {
				if (stops.length == 1) {
					return stops[0];
				}
				double u = flow == 0.0D ? clamp01(t) : pingPong(t - flow * seconds);
				double scaled = u * (stops.length - 1);
				int i = Math.min((int) scaled, stops.length - 2);
				return lerp(stops[i], stops[i + 1], scaled - i);
			}
			case RAINBOW -> {
				float hue = (float) frac(t * span + seconds * flow);
				// For rainbows the 'flow' field holds the hue speed.
				int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
				return (rainbowAlpha << 24) | rgb;
			}
			default -> {
				return stops[0];
			}
		}
	}

	/** Multiplies the alpha channel of an ARGB colour. */
	public static int withAlpha(int argb, double multiplier) {
		int a = (int) Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0D, Math.min(1.0D, multiplier)));
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	/** Raises the alpha channel to at least {@code floor} (0..1). */
	public static int withMinAlpha(int argb, double floor) {
		int a = Math.max((argb >>> 24) & 0xFF, (int) Math.round(255.0D * floor));
		return (a << 24) | (argb & 0x00FFFFFF);
	}

	public static int lerp(int from, int to, double f) {
		double k = clamp01(f);
		int a = (int) Math.round(((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * k));
		int r = (int) Math.round(((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * k));
		int g = (int) Math.round(((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * k));
		int b = (int) Math.round((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * k));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int parseHex(String text) {
		String s = text.startsWith("#") ? text.substring(1) : text;
		if (s.length() == 6) {
			return 0xFF000000 | Integer.parseInt(s, 16);
		}
		if (s.length() == 8) {
			long v = Long.parseLong(s, 16); // RRGGBBAA
			return (int) (((v & 0xFF) << 24) | (v >> 8));
		}
		throw new IllegalArgumentException("bad colour '" + text + "', expected #RRGGBB or #RRGGBBAA");
	}

	private static double clamp01(double v) {
		return Math.max(0.0D, Math.min(1.0D, v));
	}

	private static double frac(double v) {
		return v - Math.floor(v);
	}

	private static double pingPong(double v) {
		double f = frac(v / 2.0D) * 2.0D;
		return f <= 1.0D ? f : 2.0D - f;
	}
}
