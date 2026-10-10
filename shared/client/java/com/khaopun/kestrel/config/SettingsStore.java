package com.khaopun.kestrel.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.khaopun.kestrel.Kestrel;
import com.khaopun.kestrel.KestrelSettings;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and saves the behaviour settings that are not part of the theme (target filters, Mode B tuning, how many
 * candidates are predicted). They live in {@code config/kestrel/settings.json}; {@link KestrelSettings} holds the
 * live values, so this file and the config screen are the only places that change them.
 */
public final class SettingsStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private SettingsStore() {
	}

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("kestrel").resolve("settings.json");
	}

	/** Reads the file into {@link KestrelSettings}; a missing or broken file keeps the defaults (and writes them). */
	public static void load() {
		Path file = file();
		if (Files.notExists(file)) {
			save();
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject filter = obj(root, "filter");
			KestrelSettings.LOCK_PLAYERS = bool(filter, "players", KestrelSettings.LOCK_PLAYERS);
			KestrelSettings.LOCK_HOSTILE_MOBS = bool(filter, "hostileMobs", KestrelSettings.LOCK_HOSTILE_MOBS);
			KestrelSettings.LOCK_EXCLUDE_VILLAGERS = bool(filter, "excludeVillagers", KestrelSettings.LOCK_EXCLUDE_VILLAGERS);
			KestrelSettings.LOCK_EXCLUDE_TAMED = bool(filter, "excludeTamed", KestrelSettings.LOCK_EXCLUDE_TAMED);
			JsonObject lock = obj(root, "lock");
			KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS = (int) Math.round(
					num(lock, "noLineOfSightSeconds", KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS / 20.0D) * 20.0D);
			KestrelSettings.PREDICT_MAX_OTHERS = (int) num(lock, "predictedCandidates", KestrelSettings.PREDICT_MAX_OTHERS);
			JsonObject modeB = obj(root, "modeB");
			KestrelSettings.MODE_B_TURN_DEG_PER_SEC = num(modeB, "turnSpeedDegreesPerSecond", KestrelSettings.MODE_B_TURN_DEG_PER_SEC);
			KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES = num(modeB, "switchThresholdDegrees", KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES);
			KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS = (int) num(modeB, "switchCooldownTicks", KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS);
		} catch (IOException | RuntimeException e) {
			Kestrel.LOGGER.warn("Kestrel: cannot read {} ({}); keeping the current settings", file, e.getMessage());
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		JsonObject filter = new JsonObject();
		filter.addProperty("players", KestrelSettings.LOCK_PLAYERS);
		filter.addProperty("hostileMobs", KestrelSettings.LOCK_HOSTILE_MOBS);
		filter.addProperty("excludeVillagers", KestrelSettings.LOCK_EXCLUDE_VILLAGERS);
		filter.addProperty("excludeTamed", KestrelSettings.LOCK_EXCLUDE_TAMED);
		root.add("filter", filter);
		JsonObject lock = new JsonObject();
		lock.addProperty("noLineOfSightSeconds", KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS / 20.0D);
		lock.addProperty("predictedCandidates", KestrelSettings.PREDICT_MAX_OTHERS);
		root.add("lock", lock);
		JsonObject modeB = new JsonObject();
		modeB.addProperty("turnSpeedDegreesPerSecond", KestrelSettings.MODE_B_TURN_DEG_PER_SEC);
		modeB.addProperty("switchThresholdDegrees", KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES);
		modeB.addProperty("switchCooldownTicks", KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS);
		root.add("modeB", modeB);
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Kestrel.LOGGER.warn("Kestrel: cannot write {}: {}", file(), e.getMessage());
		}
	}

	private static JsonObject obj(JsonObject parent, String key) {
		return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
	}

	private static boolean bool(JsonObject o, String key, boolean fallback) {
		return o.has(key) ? o.get(key).getAsBoolean() : fallback;
	}

	private static double num(JsonObject o, String key, double fallback) {
		return o.has(key) ? o.get(key).getAsDouble() : fallback;
	}
}
