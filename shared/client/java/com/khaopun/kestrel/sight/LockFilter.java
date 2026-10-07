package com.khaopun.kestrel.sight;

import com.khaopun.kestrel.KestrelSettings;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Decides which entities may be locked on. Driven only by {@link KestrelSettings}. */
public final class LockFilter {
	private LockFilter() {
	}

	public static boolean isLockable(Entity self, Entity entity) {
		if (entity == self || !(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
			return false;
		}
		if (KestrelSettings.LOCK_EXCLUDE_VILLAGERS && entity instanceof AbstractVillager) {
			return false;
		}
		if (KestrelSettings.LOCK_EXCLUDE_TAMED && isTamed(entity)) {
			return false;
		}
		if (KestrelSettings.LOCK_PLAYERS && entity instanceof Player) {
			return true;
		}
		return KestrelSettings.LOCK_HOSTILE_MOBS && entity instanceof Enemy;
	}

	private static boolean isTamed(Entity entity) {
		return (entity instanceof TamableAnimal tamable && tamable.isTame())
				|| (entity instanceof AbstractHorse horse && horse.isTamed());
	}
}
