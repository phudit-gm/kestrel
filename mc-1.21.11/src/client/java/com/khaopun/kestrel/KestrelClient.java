package com.khaopun.kestrel;

import com.khaopun.kestrel.sight.SightRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public class KestrelClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Fires while the per-frame gizmo collector is active, so gizmos added here are drawn this frame.
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> SightRenderer.render(Minecraft.getInstance()));
	}
}
