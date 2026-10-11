package com.khaopun.kestrel.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.feedback.ThemeStore;
import com.khaopun.kestrel.feedback.theme.Theme;
import com.khaopun.kestrel.feedback.theme.ThemeLoader;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The in-game settings screen (Mod Menu > Kestrel > Config). It has no values of its own: visual and sound
 * settings are read from, and written back to, the active theme through the user's theme file; behaviour settings
 * are read from and written to {@link KestrelSettings} and saved by {@link SettingsStore}. Changes apply on Save
 * without restarting.
 *
 * <p>Needs Cloth Config; the Mod Menu entry only opens this class when Cloth Config is installed.
 */
public final class KestrelConfigScreen {
	private static final String[] FRAMES = {"box", "corners", "pixelCorners"};
	private static final String[] TRAJECTORY_STYLES = {"beads", "line", "dashed"};
	private static final String[] IMPACT_STYLES = {"box", "cross", "pixelCross"};
	private static final String[] THEMES = {"pixel_gradient", "classic"};
	private static final String[] SOUND_KEYS = {"fullCharge", "locked", "unlocked", "aligned", "switched", "modeA", "modeB"};
	private static final String[] SOUND_NAMES = {"Full charge", "Target locked", "Target lost", "Aim aligned",
			"Target switched (Mode B)", "Mode A selected", "Mode B selected"};
	private static final String[][] COLORS = {
			{"trajectory.color", "Trajectory"}, {"trajectory.colorFull", "Trajectory at full charge"},
			{"impact.color", "Impact point"}, {"candidate.color", "Candidate box (yellow)"},
			{"hit.color", "Candidate under the trajectory"}, {"locked.color", "Locked target"},
			{"aligned.color", "Aligned (green)"}, {"unlock.color", "Lost / released (red)"},
			{"ghost.color", "Predicted position"}, {"unreachable.color", "Out of reach"},
			{"label.color", "Target label"}, {"fullCharge.color", "Full charge label"}};
	private static final String[][] SHAPES = {
			{"candidate.frame", "Candidate box"}, {"hit.frame", "Candidate under the trajectory"},
			{"locked.frame", "Locked target"}, {"aligned.frame", "Aligned"}, {"unlock.frame", "Lost / released"},
			{"ghost.frame", "Predicted position"}, {"unreachable.frame", "Out of reach"}};
	private static final String[][] SWITCHES = {
			{"trajectory", "Trajectory line"}, {"impact", "Impact point"}, {"candidates", "Candidate boxes"},
			{"hit", "Candidate under the trajectory"}, {"fullCharge", "Full charge indicator"},
			{"lock", "Target lock box"}, {"ghost", "Predicted position"}, {"particles", "Particles"},
			{"sounds", "Sounds"}};

	private KestrelConfigScreen() {
	}

	public static Screen create(Screen parent) {
		Theme th = ThemeStore.theme();
		Theme dth = ThemeStore.defaults(th.name()); // what the reset buttons go back to
		Map<String, JsonElement> pending = new LinkedHashMap<>();
		String[] chosenTheme = {th.name()};

		ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.literal("Kestrel"));
		ConfigEntryBuilder eb = builder.entryBuilder();

		// ---- General
		ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
		general.addEntry(eb.startSelector(Component.literal("Theme"), THEMES, th.name())
				.setDefaultValue(ThemeLoader.DEFAULT_THEME).setSaveConsumer(v -> chosenTheme[0] = v).build());
		general.addEntry(eb.startBooleanToggle(Component.literal("Animations"), th.bool("animation.enabled"))
				.setDefaultValue(dth.bool("animation.enabled"))
				.setSaveConsumer(v -> putBool(pending, "animation.enabled", th.bool("animation.enabled"), v)).build());
		general.addEntry(eb.startDoubleField(Component.literal("Animation speed"), th.num("animation.speed"))
				.setMin(0.1D).setMax(5.0D).setDefaultValue(dth.num("animation.speed"))
				.setSaveConsumer(v -> putNum(pending, "animation.speed", th.num("animation.speed"), v)).build());
		for (String[] s : SWITCHES) {
			String path = "enabled." + s[0];
			general.addEntry(eb.startBooleanToggle(Component.literal(s[1]), th.bool(path)).setDefaultValue(dth.bool(path))
					.setSaveConsumer(v -> putBool(pending, path, th.bool(path), v)).build());
		}

		// ---- Colours
		ConfigCategory colors = builder.getOrCreateCategory(Component.literal("Colors"));
		for (String[] c : COLORS) {
			int initial = th.color(c[0]).at(0.5D, 0.0D) & 0xFFFFFF;
			int fallback = dth.color(c[0]).at(0.5D, 0.0D) & 0xFFFFFF;
			colors.addEntry(eb.startColorField(Component.literal(c[1]), initial).setDefaultValue(fallback)
					.setSaveConsumer(v -> {
						int rgb = v & 0xFFFFFF;
						if (rgb == initial) {
							return; // a colour that was not touched keeps its gradient or rainbow
						}
						if (rgb == fallback) {
							pending.put(c[0], JsonNull.INSTANCE); // back to the theme's own colour, gradient included
						} else {
							pending.put(c[0], new JsonPrimitive(String.format("#%06X", rgb)));
						}
					}).build());
		}

		// ---- Shapes
		ConfigCategory shapes = builder.getOrCreateCategory(Component.literal("Shapes"));
		for (String[] s : SHAPES) {
			shapes.addEntry(eb.startSelector(Component.literal(s[1]), FRAMES, th.str(s[0])).setDefaultValue(dth.str(s[0]))
					.setSaveConsumer(v -> putStr(pending, s[0], th.str(s[0]), v)).build());
		}
		shapes.addEntry(eb.startSelector(Component.literal("Trajectory style"), TRAJECTORY_STYLES, th.str("trajectory.style"))
				.setDefaultValue(dth.str("trajectory.style"))
				.setSaveConsumer(v -> putStr(pending, "trajectory.style", th.str("trajectory.style"), v)).build());
		shapes.addEntry(eb.startSelector(Component.literal("Impact point style"), IMPACT_STYLES, th.str("impact.style"))
				.setDefaultValue(dth.str("impact.style"))
				.setSaveConsumer(v -> putStr(pending, "impact.style", th.str("impact.style"), v)).build());

		// ---- Limits
		ConfigCategory limits = builder.getOrCreateCategory(Component.literal("Limits"));
		limits.addEntry(eb.startIntSlider(Component.literal("Max boxes shown"), (int) th.num("limits.maxCandidates"), 1, 64)
				.setDefaultValue((int) dth.num("limits.maxCandidates"))
				.setSaveConsumer(v -> putNum(pending, "limits.maxCandidates", th.num("limits.maxCandidates"), v)).build());
		limits.addEntry(eb.startIntSlider(Component.literal("Max particles per second"), (int) th.num("limits.maxParticles"), 0, 200)
				.setDefaultValue((int) dth.num("limits.maxParticles"))
				.setSaveConsumer(v -> putNum(pending, "limits.maxParticles", th.num("limits.maxParticles"), v)).build());
		limits.addEntry(eb.startIntSlider(Component.literal("Max effect updates per second"), (int) th.num("limits.maxUpdatesPerSecond"), 1, 60)
				.setDefaultValue((int) dth.num("limits.maxUpdatesPerSecond"))
				.setSaveConsumer(v -> putNum(pending, "limits.maxUpdatesPerSecond", th.num("limits.maxUpdatesPerSecond"), v)).build());
		limits.addEntry(eb.startIntSlider(Component.literal("Predicted candidates besides the target"), KestrelSettings.PREDICT_MAX_OTHERS, 0, 16)
				.setDefaultValue(KestrelSettings.Defaults.PREDICT_MAX_OTHERS).setSaveConsumer(v -> KestrelSettings.PREDICT_MAX_OTHERS = v).build());

		// ---- Targeting
		ConfigCategory targeting = builder.getOrCreateCategory(Component.literal("Targeting"));
		targeting.addEntry(eb.startTextDescription(Component.literal("Who the sight may target in Mode A (preview).")).build());
		targeting.addEntry(toggle(eb, "Players", KestrelSettings.LOCK_PLAYERS, KestrelSettings.Defaults.LOCK_PLAYERS, v -> KestrelSettings.LOCK_PLAYERS = v));
		targeting.addEntry(toggle(eb, "Hostile mobs", KestrelSettings.LOCK_HOSTILE_MOBS, KestrelSettings.Defaults.LOCK_HOSTILE_MOBS, v -> KestrelSettings.LOCK_HOSTILE_MOBS = v));
		targeting.addEntry(toggle(eb, "Animals (farm animals, fish, bats)", KestrelSettings.LOCK_ANIMALS, KestrelSettings.Defaults.LOCK_ANIMALS, v -> KestrelSettings.LOCK_ANIMALS = v));
		targeting.addEntry(toggle(eb, "Villagers and traders", KestrelSettings.LOCK_VILLAGERS, KestrelSettings.Defaults.LOCK_VILLAGERS, v -> KestrelSettings.LOCK_VILLAGERS = v));
		targeting.addEntry(toggle(eb, "Other creatures (golems, ...)", KestrelSettings.LOCK_OTHER, KestrelSettings.Defaults.LOCK_OTHER, v -> KestrelSettings.LOCK_OTHER = v));
		targeting.addEntry(toggle(eb, "Never target tamed animals", KestrelSettings.LOCK_EXCLUDE_TAMED, KestrelSettings.Defaults.LOCK_EXCLUDE_TAMED, v -> KestrelSettings.LOCK_EXCLUDE_TAMED = v));
		targeting.addEntry(eb.startStrList(Component.literal("Always target these (minecraft:creeper, or modid:* for a whole mod)"),
				KestrelSettings.LOCK_ALWAYS_IDS).setDefaultValue(java.util.List.of())
				.setSaveConsumer(v -> KestrelSettings.LOCK_ALWAYS_IDS = java.util.List.copyOf(v)).build());
		targeting.addEntry(eb.startStrList(Component.literal("Never target these"), KestrelSettings.LOCK_NEVER_IDS)
				.setDefaultValue(java.util.List.of())
				.setSaveConsumer(v -> KestrelSettings.LOCK_NEVER_IDS = java.util.List.copyOf(v)).build());
		targeting.addEntry(eb.startDoubleField(Component.literal("Release the target after this many seconds out of sight"),
				KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS / 20.0D).setMin(0.0D).setMax(30.0D)
				.setDefaultValue(KestrelSettings.Defaults.NO_LINE_OF_SIGHT_TICKS / 20.0D)
				.setSaveConsumer(v -> KestrelSettings.LOCK_NO_LINE_OF_SIGHT_TICKS = (int) Math.round(v * 20.0D)).build());
		targeting.addEntry(eb.startDoubleField(Component.literal("How long the red box stays (seconds)"), th.num("unlock.duration"))
				.setMin(0.0D).setMax(10.0D).setDefaultValue(dth.num("unlock.duration"))
				.setSaveConsumer(v -> putNum(pending, "unlock.duration", th.num("unlock.duration"), v)).build());

		// ---- Mode B
		ConfigCategory modeB = builder.getOrCreateCategory(Component.literal("Mode B"));
		modeB.addEntry(eb.startTextDescription(Component.literal(
				"Mode B turns your camera toward the target by itself. Many servers forbid aim assistance, and a server can switch Mode B off.")).build());
		modeB.addEntry(toggle(eb, "Allow Mode B", KestrelSettings.MODE_B_ENABLED, KestrelSettings.Defaults.MODE_B_ENABLED, v -> KestrelSettings.MODE_B_ENABLED = v));
		modeB.addEntry(toggle(eb, "Remind me about server rules when I use Mode B on a server", KestrelSettings.MODE_B_SERVER_WARNING, KestrelSettings.Defaults.MODE_B_SERVER_WARNING,
				v -> KestrelSettings.MODE_B_SERVER_WARNING = v));
		modeB.addEntry(toggle(eb, "Aim for the head (the middle of the hitbox while the target is in the air or the head is covered)", KestrelSettings.MODE_B_AIM_HEAD, KestrelSettings.Defaults.MODE_B_AIM_HEAD, v -> KestrelSettings.MODE_B_AIM_HEAD = v));
		modeB.addEntry(eb.startTextDescription(Component.literal("Who Mode B may target (instead of the Mode A list):")).build());
		modeB.addEntry(toggle(eb, "Players", KestrelSettings.MODE_B_LOCK_PLAYERS, KestrelSettings.Defaults.MODE_B_LOCK_PLAYERS, v -> KestrelSettings.MODE_B_LOCK_PLAYERS = v));
		modeB.addEntry(toggle(eb, "Hostile mobs", KestrelSettings.MODE_B_LOCK_HOSTILE_MOBS, KestrelSettings.Defaults.MODE_B_LOCK_HOSTILE_MOBS, v -> KestrelSettings.MODE_B_LOCK_HOSTILE_MOBS = v));
		modeB.addEntry(toggle(eb, "Animals", KestrelSettings.MODE_B_LOCK_ANIMALS, KestrelSettings.Defaults.MODE_B_LOCK_ANIMALS, v -> KestrelSettings.MODE_B_LOCK_ANIMALS = v));
		modeB.addEntry(toggle(eb, "Villagers and traders", KestrelSettings.MODE_B_LOCK_VILLAGERS, KestrelSettings.Defaults.MODE_B_LOCK_VILLAGERS, v -> KestrelSettings.MODE_B_LOCK_VILLAGERS = v));
		modeB.addEntry(toggle(eb, "Other creatures", KestrelSettings.MODE_B_LOCK_OTHER, KestrelSettings.Defaults.MODE_B_LOCK_OTHER, v -> KestrelSettings.MODE_B_LOCK_OTHER = v));
		modeB.addEntry(eb.startDoubleField(Component.literal("Camera turn speed (degrees per second)"), KestrelSettings.MODE_B_TURN_DEG_PER_SEC)
				.setMin(30.0D).setMax(2000.0D).setDefaultValue(KestrelSettings.Defaults.MODE_B_TURN_DEG_PER_SEC)
				.setSaveConsumer(v -> KestrelSettings.MODE_B_TURN_DEG_PER_SEC = v).build());
		modeB.addEntry(eb.startDoubleField(Component.literal("Mouse movement that switches target (degrees)"), KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES)
				.setMin(1.0D).setMax(90.0D).setDefaultValue(KestrelSettings.Defaults.MODE_B_SWITCH_THRESHOLD_DEGREES)
				.setSaveConsumer(v -> KestrelSettings.MODE_B_SWITCH_THRESHOLD_DEGREES = v).build());
		modeB.addEntry(eb.startIntSlider(Component.literal("Pause after a switch (ticks)"), KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS, 0, 40)
				.setDefaultValue(KestrelSettings.Defaults.MODE_B_SWITCH_COOLDOWN_TICKS).setSaveConsumer(v -> KestrelSettings.MODE_B_SWITCH_COOLDOWN_TICKS = v).build());

		limits.addEntry(eb.startBooleanToggle(Component.literal("Log every shot's accuracy ([kestrel-shot] in latest.log)"), KestrelSettings.DEBUG_SHOT_LOG)
				.setDefaultValue(KestrelSettings.Defaults.DEBUG_SHOT_LOG).setSaveConsumer(v -> KestrelSettings.DEBUG_SHOT_LOG = v).build());

		// ---- Sounds
		ConfigCategory sounds = builder.getOrCreateCategory(Component.literal("Sounds"));
		for (int i = 0; i < SOUND_KEYS.length; i++) {
			String base = "sounds." + SOUND_KEYS[i] + ".";
			sounds.addEntry(eb.startBooleanToggle(Component.literal(SOUND_NAMES[i]), th.bool(base + "enabled"))
					.setDefaultValue(dth.bool(base + "enabled"))
					.setSaveConsumer(v -> putBool(pending, base + "enabled", th.bool(base + "enabled"), v)).build());
			sounds.addEntry(eb.startStrField(Component.literal("   sound id"), th.str(base + "id")).setDefaultValue(dth.str(base + "id"))
					.setSaveConsumer(v -> putStr(pending, base + "id", th.str(base + "id"), v.trim())).build());
			sounds.addEntry(eb.startDoubleField(Component.literal("   volume"), th.num(base + "volume")).setMin(0.0D).setMax(2.0D)
					.setDefaultValue(dth.num(base + "volume"))
					.setSaveConsumer(v -> putNum(pending, base + "volume", th.num(base + "volume"), v)).build());
			sounds.addEntry(eb.startDoubleField(Component.literal("   pitch"), th.num(base + "pitch")).setMin(0.5D).setMax(2.0D)
					.setDefaultValue(dth.num(base + "pitch"))
					.setSaveConsumer(v -> putNum(pending, base + "pitch", th.num(base + "pitch"), v)).build());
		}

		builder.setSavingRunnable(() -> {
			SettingsStore.save();
			UserThemeFile.apply(chosenTheme[0], pending);
		});
		return builder.build();
	}

	private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<Boolean> toggle(ConfigEntryBuilder eb, String name,
			boolean value, boolean fallback, java.util.function.Consumer<Boolean> save) {
		return eb.startBooleanToggle(Component.literal(name), value).setDefaultValue(fallback).setSaveConsumer(save::accept).build();
	}

	private static void putBool(Map<String, JsonElement> pending, String path, boolean initial, boolean value) {
		if (value != initial) {
			pending.put(path, new JsonPrimitive(value));
		}
	}

	private static void putNum(Map<String, JsonElement> pending, String path, double initial, double value) {
		if (Math.abs(value - initial) > 1.0E-9D) {
			pending.put(path, new JsonPrimitive(value));
		}
	}

	private static void putStr(Map<String, JsonElement> pending, String path, String initial, String value) {
		if (!Objects.equals(value, initial)) {
			pending.put(path, new JsonPrimitive(value));
		}
	}
}
