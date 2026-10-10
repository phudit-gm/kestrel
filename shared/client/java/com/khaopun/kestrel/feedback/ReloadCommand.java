package com.khaopun.kestrel.feedback;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;

/** Body of {@code /kestrel reload}; the per-version client registers the command and passes in its reply channel. */
public final class ReloadCommand {
	private ReloadCommand() {
	}

	public static int run(Consumer<Component> reply) {
		com.khaopun.kestrel.config.SettingsStore.load();
		List<String> problems = ThemeStore.reload();
		reply.accept(Component.translatable("kestrel.command.reload.done", ThemeStore.theme().name()));
		for (String problem : problems) {
			reply.accept(Component.translatable("kestrel.command.reload.problem", problem));
		}
		return 1;
	}
}
