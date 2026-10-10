package com.khaopun.kestrel;

import com.khaopun.kestrel.feedback.FeedbackLayer;
import com.khaopun.kestrel.feedback.ReloadCommand;
import com.khaopun.kestrel.sight.AimMode;
import com.khaopun.kestrel.sight.ModeKey;
import com.khaopun.kestrel.sight.SightController;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public class KestrelClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FeedbackLayer.init();
		AimMode.setMessenger((player, text) -> player.displayClientMessage(text, true));
		ModeKey.register(KeyBindingHelper::registerKeyBinding);
		// Fires while the per-frame gizmo collector is active, so gizmos added here are drawn this frame.
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> SightController.update(Minecraft.getInstance()));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("kestrel").then(ClientCommandManager.literal("reload")
						.executes(context -> ReloadCommand.run(context.getSource()::sendFeedback)))));
	}
}
