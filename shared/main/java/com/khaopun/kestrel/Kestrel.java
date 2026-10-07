package com.khaopun.kestrel;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kestrel adds no registry entries (no items, blocks, or entities), so players without the mod can still join
 * servers that have it, and the mod works on a vanilla dedicated server.
 */
public class Kestrel implements ModInitializer {
	public static final String MOD_ID = "kestrel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Nothing to register. Server-side parts (flight) arrive in a later step.
	}
}
