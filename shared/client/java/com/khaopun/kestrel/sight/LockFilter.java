package com.khaopun.kestrel.sight;

import java.util.List;

import com.khaopun.kestrel.KestrelSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Decides which entities may be locked on. Driven by {@link KestrelSettings} and {@link ServerPolicy}. */
public final class LockFilter {
	private enum Kind {
		PLAYER, HOSTILE, ANIMAL, VILLAGER, OTHER
	}

	private LockFilter() {
	}

	public static boolean isLockable(Entity self, Entity entity) {
		if (entity == self || !(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (entity instanceof Player && !ServerPolicy.allowPlayers(mc)) {
			return false;
		}
		if (KestrelSettings.LOCK_EXCLUDE_TAMED && isTamed(entity)) {
			return false;
		}
		String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
		if (matches(KestrelSettings.LOCK_NEVER_IDS, id)) {
			return false;
		}
		if (matches(KestrelSettings.LOCK_ALWAYS_IDS, id)) {
			return true;
		}
		boolean modeB = AimMode.modeB(mc);
		return switch (kind(entity)) {
			case PLAYER -> modeB ? KestrelSettings.MODE_B_LOCK_PLAYERS : KestrelSettings.LOCK_PLAYERS;
			case HOSTILE -> modeB ? KestrelSettings.MODE_B_LOCK_HOSTILE_MOBS : KestrelSettings.LOCK_HOSTILE_MOBS;
			case ANIMAL -> modeB ? KestrelSettings.MODE_B_LOCK_ANIMALS : KestrelSettings.LOCK_ANIMALS;
			case VILLAGER -> modeB ? KestrelSettings.MODE_B_LOCK_VILLAGERS : KestrelSettings.LOCK_VILLAGERS;
			case OTHER -> modeB ? KestrelSettings.MODE_B_LOCK_OTHER : KestrelSettings.LOCK_OTHER;
		};
	}

	private static Kind kind(Entity entity) {
		if (entity instanceof Player) {
			return Kind.PLAYER;
		}
		if (entity instanceof Enemy) {
			return Kind.HOSTILE;
		}
		if (entity instanceof AbstractVillager) {
			return Kind.VILLAGER;
		}
		MobCategory category = entity.getType().getCategory();
		return switch (category) {
			case MONSTER -> Kind.HOSTILE;
			case CREATURE, AMBIENT, AXOLOTLS, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, WATER_AMBIENT -> Kind.ANIMAL;
			default -> Kind.OTHER;
		};
	}

	/** An entry is a full id (minecraft:creeper), a bare name (creeper = minecraft:creeper) or modid:* for a whole mod. */
	static boolean matches(List<String> entries, String id) {
		for (String raw : entries) {
			String entry = raw.trim().toLowerCase(java.util.Locale.ROOT);
			if (entry.isEmpty()) {
				continue;
			}
			if (entry.endsWith(":*")) {
				if (id.startsWith(entry.substring(0, entry.length() - 1))) {
					return true;
				}
			} else if (id.equals(entry.contains(":") ? entry : "minecraft:" + entry)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isTamed(Entity entity) {
		return (entity instanceof TamableAnimal tamable && tamable.isTame())
				|| (entity instanceof AbstractHorse horse && horse.isTamed());
	}
}
