package com.khaopun.kestrel.feedback.theme;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;

/**
 * Builds a {@link Theme} from the bundled theme files and the user's theme file. Pure data handling: no game
 * classes.
 *
 * <p>User file format: {@code theme} picks the base theme, {@code overrides} is a map of dotted paths to values,
 * and every other top-level object ({@code enabled}, {@code animation}, {@code limits}, ...) is merged over the
 * theme at the same place.
 */
public final class ThemeLoader {
	public static final String FALLBACK_THEME = "classic";
	public static final String DEFAULT_THEME = "pixel_gradient";

	private ThemeLoader() {
	}

	/**
	 * @param bundled  returns the JSON text of a bundled theme by name, or null if there is none
	 * @param userJson the user's theme file, or null if absent
	 * @param message  receives one-line problems to show the player
	 */
	public static Theme load(Function<String, String> bundled, String userJson, Logger log, Consumer<String> message) {
		JsonObject fallback = parseBundled(bundled, FALLBACK_THEME, log);
		JsonObject user = new JsonObject();
		if (userJson != null) {
			try {
				JsonElement parsed = JsonParser.parseString(userJson);
				if (parsed.isJsonObject()) {
					user = parsed.getAsJsonObject();
				} else {
					message.accept("theme.json must be a JSON object; using defaults");
				}
			} catch (RuntimeException e) {
				log.warn("Kestrel: cannot parse theme.json: {}", e.getMessage());
				message.accept("theme.json has a syntax error (" + firstLine(e.getMessage()) + "); using defaults");
			}
		}

		String requested = user.has("theme") && user.get("theme").isJsonPrimitive() ? user.get("theme").getAsString() : DEFAULT_THEME;
		if ("khaopun".equals(requested)) {
			requested = DEFAULT_THEME; // the default theme's old name, kept so existing theme.json files still work
		}
		String used = requested;
		JsonObject base = FALLBACK_THEME.equals(requested) ? fallback.deepCopy() : bundledOrNull(bundled, requested, log);
		if (base == null) {
			message.accept("unknown theme '" + requested + "'; using " + DEFAULT_THEME);
			used = DEFAULT_THEME;
			base = bundledOrNull(bundled, DEFAULT_THEME, log);
			if (base == null) {
				base = fallback.deepCopy();
				used = FALLBACK_THEME;
			}
		}

		for (Map.Entry<String, JsonElement> entry : user.entrySet()) {
			String key = entry.getKey();
			if (!key.equals("theme") && !key.equals("overrides")) {
				mergeInto(base, key, entry.getValue());
			}
		}
		if (user.has("overrides") && user.get("overrides").isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : user.getAsJsonObject("overrides").entrySet()) {
				setPath(base, entry.getKey(), entry.getValue());
			}
		}
		return new Theme(used, base, fallback, log);
	}

	private static void mergeInto(JsonObject target, String key, JsonElement value) {
		JsonElement existing = target.get(key);
		if (value.isJsonObject() && existing != null && existing.isJsonObject()) {
			for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet()) {
				mergeInto(existing.getAsJsonObject(), e.getKey(), e.getValue());
			}
		} else {
			target.add(key, value.deepCopy());
		}
	}

	private static void setPath(JsonObject root, String dottedPath, JsonElement value) {
		String[] parts = dottedPath.split("\\.");
		JsonObject current = root;
		for (int i = 0; i < parts.length - 1; i++) {
			JsonElement next = current.get(parts[i]);
			if (next == null || !next.isJsonObject()) {
				next = new JsonObject();
				current.add(parts[i], next);
			}
			current = next.getAsJsonObject();
		}
		current.add(parts[parts.length - 1], value.deepCopy());
	}

	private static JsonObject parseBundled(Function<String, String> bundled, String name, Logger log) {
		JsonObject o = bundledOrNull(bundled, name, log);
		return o == null ? new JsonObject() : o;
	}

	private static JsonObject bundledOrNull(Function<String, String> bundled, String name, Logger log) {
		String text = bundled.apply(name);
		if (text == null) {
			return null;
		}
		try {
			return JsonParser.parseString(text).getAsJsonObject();
		} catch (RuntimeException e) {
			log.error("Kestrel: bundled theme '{}' is broken: {}", name, e.getMessage());
			return null;
		}
	}

	private static String firstLine(String s) {
		if (s == null) {
			return "unknown";
		}
		int i = s.indexOf('\n');
		return i < 0 ? s : s.substring(0, i);
	}
}
