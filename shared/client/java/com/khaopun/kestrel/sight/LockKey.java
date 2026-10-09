package com.khaopun.kestrel.sight;

import java.util.function.UnaryOperator;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** The rebindable lock key (Controls > Kestrel) and the per-tick hook that drives {@link LockController}. */
public final class LockKey {
	private static KeyMapping lockKey;

	private LockKey() {
	}

	/** @param registrar Fabric's key registration, whose class name differs between game versions */
	public static void register(UnaryOperator<KeyMapping> registrar) {
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("kestrel", "main"));
		lockKey = registrar.apply(new KeyMapping("key.kestrel.lock", InputConstants.KEY_R, category));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (lockKey.consumeClick()) {
				LockController.onKey(client);
			}
			LockController.onTick(client);
		});
	}
}
