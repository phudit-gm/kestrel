package com.khaopun.kestrel.sight;

import java.util.function.UnaryOperator;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** The rebindable mode key (Controls > Kestrel) and the per-tick hook that drives {@link LockController} and {@link ModeB}. */
public final class ModeKey {
	private static KeyMapping modeKey;

	private ModeKey() {
	}

	/** @param registrar Fabric's key registration, whose class name differs between game versions */
	public static void register(UnaryOperator<KeyMapping> registrar) {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kestrel", "main"));
		modeKey = registrar.apply(new KeyMapping("key.kestrel.mode", InputConstants.KEY_R, category));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (modeKey.consumeClick()) {
				AimMode.toggle(client);
			}
			LockController.onTick(client);
			ModeB.onTick(client);
			DebugShots.onTick(client);
		});
	}
}
