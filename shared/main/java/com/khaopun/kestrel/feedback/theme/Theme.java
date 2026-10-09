package com.khaopun.kestrel.feedback.theme;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.slf4j.Logger;

/**
 * A resolved theme: the selected theme file with the user's switches and overrides merged in. Values are looked up
 * by dotted path ({@code "candidate.color"}). A path missing from the selected theme falls back to the fallback
 * theme (classic); a path missing from both is logged once and reads as zero / empty / transparent.
 */
public final class Theme {
	private final String name;
	private final JsonObject tree;
	private final JsonObject fallback;
	private final Logger log;
	private final Map<String, Object> cache = new ConcurrentHashMap<>();
	private final Set<String> reported = ConcurrentHashMap.newKeySet();

	Theme(String name, JsonObject tree, JsonObject fallback, Logger log) {
		this.name = name;
		this.tree = tree;
		this.fallback = fallback;
		this.log = log;
	}

	public String name() {
		return name;
	}

	public double num(String path) {
		return (Double) cache.computeIfAbsent("n:" + path, k -> {
			JsonElement e = find(path);
			try {
				return e == null ? 0.0D : e.getAsDouble();
			} catch (RuntimeException ex) {
				return bad(path, ex, 0.0D);
			}
		});
	}

	public int integer(String path) {
		return (int) Math.round(num(path));
	}

	public boolean bool(String path) {
		return (Boolean) cache.computeIfAbsent("b:" + path, k -> {
			JsonElement e = find(path);
			try {
				return e != null && e.getAsBoolean();
			} catch (RuntimeException ex) {
				return bad(path, ex, false);
			}
		});
	}

	public String str(String path) {
		return (String) cache.computeIfAbsent("s:" + path, k -> {
			JsonElement e = find(path);
			try {
				return e == null ? "" : e.getAsString();
			} catch (RuntimeException ex) {
				return bad(path, ex, "");
			}
		});
	}

	public ColorSpec color(String path) {
		return (ColorSpec) cache.computeIfAbsent("c:" + path, k -> {
			JsonElement e = find(path);
			try {
				return e == null ? ColorSpec.solid(0) : ColorSpec.parse(e);
			} catch (RuntimeException ex) {
				// A bad user colour must not break the whole theme: use the fallback theme's value.
				JsonElement fb = lookup(fallback, path);
				if (fb != null) {
					try {
						log.warn("Kestrel theme: bad colour at '{}' ({}), using the classic value", path, ex.getMessage());
						return ColorSpec.parse(fb);
					} catch (RuntimeException ignored) {
						// fall through
					}
				}
				return bad(path, ex, ColorSpec.solid(0));
			}
		});
	}

	/** True if the on/off switch for an element group is on. */
	public boolean enabled(String group) {
		return bool("enabled." + group);
	}

	private <T> T bad(String path, RuntimeException ex, T value) {
		log.warn("Kestrel theme: invalid value at '{}': {}", path, ex.getMessage());
		return value;
	}

	private JsonElement find(String path) {
		JsonElement e = lookup(tree, path);
		if (e == null) {
			e = lookup(fallback, path);
		}
		if (e == null && reported.add(path)) {
			log.warn("Kestrel theme: no value for '{}' in theme '{}' or the fallback theme", path, name);
		}
		return e;
	}

	private static JsonElement lookup(JsonObject root, String path) {
		JsonElement current = root;
		for (String part : path.split("\\.")) {
			if (current == null || !current.isJsonObject()) {
				return null;
			}
			current = current.getAsJsonObject().get(part);
		}
		return current;
	}
}
