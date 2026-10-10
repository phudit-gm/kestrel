package com.khaopun.kestrel;

import com.khaopun.kestrel.config.SettingsStore;
import com.khaopun.kestrel.feedback.FeedbackLayer;
import com.khaopun.kestrel.feedback.ReloadCommand;
import com.khaopun.kestrel.sight.AimMode;
import com.khaopun.kestrel.sight.HeldWeapon;
import com.khaopun.kestrel.sight.ModeKey;
import com.khaopun.kestrel.sight.SightController;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

public class KestrelClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SettingsStore.load();
		FeedbackLayer.init();
		HeldWeapon.setProjectileReader(charged -> charged.itemCopies());
		AimMode.setMessenger((player, text) -> player.sendOverlayMessage(text));
		AimMode.setChatMessenger((player, text) -> player.sendSystemMessage(text));
		ModeKey.register(KeyMappingHelper::registerKeyMapping);
		// Fires while the per-frame gizmo collector is active, so gizmos added here are drawn this frame.
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> SightController.update(Minecraft.getInstance()));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommands.literal("kestrel").then(ClientCommands.literal("reload")
						.executes(context -> ReloadCommand.run(context.getSource()::sendFeedback)))));
	}
}
