package com.khaopun.kestrel.sight;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.BiConsumer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Mode A (preview and prediction) or Mode B (camera turns to the aim point). Saved in the config folder. */
public final class AimMode {
	private static boolean modeB;
	private static boolean loaded;
	private static BiConsumer<LocalPlayer, Component> messenger = (player, text) -> {
	};

	private AimMode() {
	}

	/** @param messenger shows a short line above the hotbar; the method for that differs between game versions */
	public static void setMessenger(BiConsumer<LocalPlayer, Component> messenger) {
		AimMode.messenger = messenger;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("kestrel.properties");
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(file())) {
			p.load(in);
			modeB = "B".equalsIgnoreCase(p.getProperty("mode", "A").trim());
		} catch (IOException e) {
			modeB = false; // first run
		}
	}

	private static void save() {
		Properties p = new Properties();
		p.setProperty("mode", modeB ? "B" : "A");
		try (OutputStream out = Files.newOutputStream(file())) {
			p.store(out, "Kestrel");
		} catch (IOException e) {
			// not being able to save only means the mode is forgotten on restart
		}
	}

	/** Mode B is only permitted in singleplayer for now (Step 4 would add server permission). */
	public static boolean modeBPermitted(Minecraft mc) {
		return mc.hasSingleplayerServer();
	}

	/** True if Mode B is both selected and permitted. */
	public static boolean modeB(Minecraft mc) {
		load();
		return modeB && modeBPermitted(mc);
	}

	public static void toggle(Minecraft mc) {
		load();
		if (!modeB && !modeBPermitted(mc)) {
			message(mc, "Kestrel: Mode B is only available in singleplayer");
			return;
		}
		modeB = !modeB;
		save();
		message(mc, modeB ? "Kestrel: Mode B (auto aim)" : "Kestrel: Mode A (preview)");
	}

	private static void message(Minecraft mc, String text) {
		if (mc.player != null) {
			messenger.accept(mc.player, Component.literal(text));
		}
	}
}
