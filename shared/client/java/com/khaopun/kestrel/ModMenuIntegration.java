package com.khaopun.kestrel;

import com.khaopun.kestrel.config.KestrelConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.loader.api.FabricLoader;

/** Adds the config button in Mod Menu. Mod Menu loads this class only if it is installed. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// Cloth Config draws the screen; without it the button simply does nothing.
		return parent -> FabricLoader.getInstance().isModLoaded("cloth-config") ? KestrelConfigScreen.create(parent) : parent;
	}
}
