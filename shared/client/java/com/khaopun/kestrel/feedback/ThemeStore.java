package com.khaopun.kestrel.feedback;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.khaopun.kestrel.Kestrel;
import com.khaopun.kestrel.feedback.theme.Theme;
import com.khaopun.kestrel.feedback.theme.ThemeLoader;

import net.fabricmc.loader.api.FabricLoader;

/** Owns the active theme and the user's theme file ({@code config/kestrel/theme.json}). */
public final class ThemeStore {
	private static final String USER_FILE_TEMPLATE = """
			{
			  "theme": "khaopun",
			  "enabled": {
			    "trajectory": true,
			    "impact": true,
			    "candidates": true,
			    "hit": true,
			    "fullCharge": true,
			    "particles": true,
			    "sounds": true
			  },
			  "overrides": {
			  }
			}
			""";

	private static volatile Theme current;

	private ThemeStore() {
	}

	public static Path userFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("kestrel").resolve("theme.json");
	}

	public static Theme theme() {
		Theme theme = current;
		if (theme == null) {
			reload();
			theme = current;
		}
		return theme;
	}

	/** Re-reads the theme file. Returns problems to show the player; empty if everything loaded cleanly. */
	public static synchronized List<String> reload() {
		List<String> problems = new ArrayList<>();
		Path file = userFile();
		String userJson = null;
		try {
			if (Files.notExists(file)) {
				Files.createDirectories(file.getParent());
				Files.writeString(file, USER_FILE_TEMPLATE, StandardCharsets.UTF_8);
			}
			userJson = Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			Kestrel.LOGGER.warn("Kestrel: cannot read {}: {}", file, e.getMessage());
			problems.add("cannot read theme.json: " + e.getMessage());
		}
		current = ThemeLoader.load(ThemeStore::bundled, userJson, Kestrel.LOGGER, problems::add);
		FeedbackLayer.onThemeReloaded();
		return problems;
	}

	private static String bundled(String name) {
		if (!name.matches("[A-Za-z0-9_-]+")) {
			return null;
		}
		try (InputStream in = ThemeStore.class.getResourceAsStream("/assets/kestrel/themes/" + name + ".json")) {
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return null;
		}
	}
}
