package com.khaopun.kestrel.sight;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.BiConsumer;

import com.khaopun.kestrel.KestrelSettings;
import com.khaopun.kestrel.feedback.FeedbackEvent;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Mode A (preview and prediction) or Mode B (camera turns to the aim point). Saved in the config folder. */
public final class AimMode {
	private static boolean modeB;
	private static boolean loaded;
	private static boolean warned;
	private static final String WARNING = "Kestrel: Mode B turns your camera toward the target by itself. Many servers, "
			+ "especially PvP and competitive ones, forbid aim assistance like this. Check the server rules before you use it. "
			+ "(Turn this reminder off in Kestrel settings > Mode B.)";
	private static BiConsumer<LocalPlayer, Component> messenger = (player, text) -> {
	};

	private static BiConsumer<LocalPlayer, Component> chatMessenger = (player, text) -> {
	};

	private AimMode() {
	}

	/** @param messenger shows a short line above the hotbar; the method for that differs between game versions */
	public static void setMessenger(BiConsumer<LocalPlayer, Component> messenger) {
		AimMode.messenger = messenger;
	}

	/** @param messenger writes a line into the chat; the method for that differs between game versions */
	public static void setChatMessenger(BiConsumer<LocalPlayer, Component> messenger) {
		AimMode.chatMessenger = messenger;
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

	/** Mode B needs the player's own switch on and the server not to have switched it off ({@link ServerPolicy}). */
	public static boolean modeBPermitted(Minecraft mc) {
		return KestrelSettings.MODE_B_ENABLED && ServerPolicy.allowModeB(mc);
	}

	/** True if Mode B is both selected and permitted. */
	public static boolean modeB(Minecraft mc) {
		load();
		return modeB && modeBPermitted(mc);
	}

	public static void toggle(Minecraft mc) {
		load();
		if (!modeB && !modeBPermitted(mc)) {
			message(mc, KestrelSettings.MODE_B_ENABLED ? "Kestrel: this server does not allow Mode B"
					: "Kestrel: Mode B is turned off in the settings");
			return;
		}
		modeB = !modeB;
		save();
		if (modeB) {
			warnOnce(mc);
		}
		message(mc, modeB ? "Kestrel: Mode B (auto aim)" : "Kestrel: Mode A (preview)");
		LockController.announce(modeB ? FeedbackEvent.MODE_B_SELECTED : FeedbackEvent.MODE_A_SELECTED);
	}

	/** Shows the fair-play reminder in chat once per session, on a multiplayer server, when Mode B is in use. */
	public static void warnOnce(Minecraft mc) {
		if (KestrelSettings.MODE_B_SERVER_WARNING && !warned && !mc.hasSingleplayerServer() && mc.player != null) {
			warned = true;
			chatMessenger.accept(mc.player, Component.literal(WARNING));
		}
	}

	private static void message(Minecraft mc, String text) {
		if (mc.player != null) {
			messenger.accept(mc.player, Component.literal(text));
		}
	}
}
