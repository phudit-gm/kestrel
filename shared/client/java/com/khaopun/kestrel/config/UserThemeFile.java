package com.khaopun.kestrel.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.khaopun.kestrel.Kestrel;
import com.khaopun.kestrel.feedback.ThemeStore;

/**
 * Writes the config screen's visual choices into the user's theme file ({@code config/kestrel/theme.json}), the
 * same file the player can edit by hand: the chosen theme goes to {@code theme} and every changed value to
 * {@code overrides}, which wins over everything else. Then the theme is reloaded, so changes show at once.
 */
public final class UserThemeFile {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private UserThemeFile() {
	}

	/** @param overrides dotted theme path to the new value; only values that changed should be in it. A JSON null removes the override. */
	public static void apply(String themeName, Map<String, JsonElement> overrides) {
		Path file = ThemeStore.userFile();
		JsonObject root = new JsonObject();
		try {
			if (Files.exists(file)) {
				root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			}
		} catch (IOException | RuntimeException e) {
			Kestrel.LOGGER.warn("Kestrel: theme.json is unreadable ({}); the config screen writes a fresh one", e.getMessage());
			root = new JsonObject();
		}
		root.addProperty("theme", themeName);
		JsonObject existing = root.has("overrides") && root.get("overrides").isJsonObject()
				? root.getAsJsonObject("overrides") : new JsonObject();
		for (Map.Entry<String, JsonElement> e : overrides.entrySet()) {
			if (e.getValue().isJsonNull()) {
				existing.remove(e.getKey());
			} else {
				existing.add(e.getKey(), e.getValue());
			}
		}
		root.add("overrides", existing);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Kestrel.LOGGER.warn("Kestrel: cannot write {}: {}", file, e.getMessage());
		}
		ThemeStore.reload();
	}
}
