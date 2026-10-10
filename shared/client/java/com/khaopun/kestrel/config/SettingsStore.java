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
			KestrelSettings.LOCK_ANIMALS = bool(filter, "animals", KestrelSettings.LOCK_ANIMALS);
			KestrelSettings.LOCK_VILLAGERS = bool(filter, "villagers", KestrelSettings.LOCK_VILLAGERS);
			KestrelSettings.LOCK_OTHER = bool(filter, "other", KestrelSettings.LOCK_OTHER);
			KestrelSettings.LOCK_EXCLUDE_TAMED = bool(filter, "excludeTamed", KestrelSettings.LOCK_EXCLUDE_TAMED);
			KestrelSettings.LOCK_ALWAYS_IDS = strings(filter, "alwaysIds");
			KestrelSettings.LOCK_NEVER_IDS = strings(filter, "neverIds");
			JsonObject bFilter = obj(root, "modeBFilter");
			KestrelSettings.MODE_B_LOCK_PLAYERS = bool(bFilter, "players", KestrelSettings.MODE_B_LOCK_PLAYERS);
			KestrelSettings.MODE_B_LOCK_HOSTILE_MOBS = bool(bFilter, "hostileMobs", KestrelSettings.MODE_B_LOCK_HOSTILE_MOBS);
			KestrelSettings.MODE_B_LOCK_ANIMALS = bool(bFilter, "animals", KestrelSettings.MODE_B_LOCK_ANIMALS);
			KestrelSettings.MODE_B_LOCK_VILLAGERS = bool(bFilter, "villagers", KestrelSettings.MODE_B_LOCK_VILLAGERS);
			KestrelSettings.MODE_B_LOCK_OTHER = bool(bFilter, "other", KestrelSettings.MODE_B_LOCK_OTHER);
			JsonObject lock = obj(root, "lock");
			KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS = (int) Math.round(
					num(lock, "noLineOfSightSeconds", KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS / 20.0D) * 20.0D);
			KestrelSettings.PREDICT_MAX_OTHERS = (int) num(lock, "predictedCandidates", KestrelSettings.PREDICT_MAX_OTHERS);
			KestrelSettings.DEBUG_SHOT_LOG = bool(obj(root, "debug"), "shotLog", KestrelSettings.DEBUG_SHOT_LOG);
			JsonObject modeB = obj(root, "modeB");
			KestrelSettings.MODE_B_ENABLED = bool(modeB, "enabled", KestrelSettings.MODE_B_ENABLED);
			KestrelSettings.MODE_B_SERVER_WARNING = bool(modeB, "serverWarning", KestrelSettings.MODE_B_SERVER_WARNING);
			KestrelSettings.MODE_B_AIM_HEAD = bool(modeB, "aimHead", KestrelSettings.MODE_B_AIM_HEAD);
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
		filter.addProperty("animals", KestrelSettings.LOCK_ANIMALS);
		filter.addProperty("villagers", KestrelSettings.LOCK_VILLAGERS);
		filter.addProperty("other", KestrelSettings.LOCK_OTHER);
		filter.addProperty("excludeTamed", KestrelSettings.LOCK_EXCLUDE_TAMED);
		filter.add("alwaysIds", array(KestrelSettings.LOCK_ALWAYS_IDS));
		filter.add("neverIds", array(KestrelSettings.LOCK_NEVER_IDS));
		root.add("filter", filter);
		JsonObject bFilter = new JsonObject();
		bFilter.addProperty("players", KestrelSettings.MODE_B_LOCK_PLAYERS);
		bFilter.addProperty("hostileMobs", KestrelSettings.MODE_B_LOCK_HOSTILE_MOBS);
		bFilter.addProperty("animals", KestrelSettings.MODE_B_LOCK_ANIMALS);
		bFilter.addProperty("villagers", KestrelSettings.MODE_B_LOCK_VILLAGERS);
		bFilter.addProperty("other", KestrelSettings.MODE_B_LOCK_OTHER);
		root.add("modeBFilter", bFilter);
		JsonObject lock = new JsonObject();
		lock.addProperty("noLineOfSightSeconds", KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS / 20.0D);
		lock.addProperty("predictedCandidates", KestrelSettings.PREDICT_MAX_OTHERS);
		root.add("lock", lock);
		JsonObject modeB = new JsonObject();
		modeB.addProperty("enabled", KestrelSettings.MODE_B_ENABLED);
		modeB.addProperty("serverWarning", KestrelSettings.MODE_B_SERVER_WARNING);
		modeB.addProperty("aimHead", KestrelSettings.MODE_B_AIM_HEAD);
		modeB.addProperty("turnSpeedDegreesPerSecond", KestrelSettings.MODE_B_TURN_DEG_PER_SEC);
		modeB.addProperty("switchThresholdDegrees", KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES);
		modeB.addProperty("switchCooldownTicks", KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS);
		root.add("modeB", modeB);
		JsonObject debug = new JsonObject();
		debug.addProperty("shotLog", KestrelSettings.DEBUG_SHOT_LOG);
		root.add("debug", debug);
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			Kestrel.LOGGER.warn("Kestrel: cannot write {}: {}", file(), e.getMessage());
		}
	}

	private static java.util.List<String> strings(JsonObject o, String key) {
		java.util.List<String> out = new java.util.ArrayList<>();
		if (o.has(key) && o.get(key).isJsonArray()) {
			o.getAsJsonArray(key).forEach(e -> out.add(e.getAsString()));
		}
		return java.util.List.copyOf(out);
	}

	private static com.google.gson.JsonArray array(java.util.List<String> values) {
		com.google.gson.JsonArray array = new com.google.gson.JsonArray();
		values.forEach(array::add);
		return array;
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
