package com.khaopun.kestrel.sight;

import java.util.List;
import java.util.function.Function;

import com.khaopun.kestrel.core.ArrowPhysics;
import com.khaopun.kestrel.core.FlightModel;
import com.khaopun.kestrel.core.ShotProfile;
import com.khaopun.kestrel.core.WeaponPhysics;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import org.jspecify.annotations.Nullable;

/**
 * The one place that decides which weapon the player is aiming and what shot it would make right now.
 *
 * <pre>
 * Pseudocode (see also brief/step5-pseudocode.md):
 *
 *   if the player is using an item:
 *     bow:      power from draw time; none below the minimum power; arrow, speed = power * 3, adds own motion
 *     trident:  none for Riptide (it launches the player instead of throwing) and none before the throw threshold;
 *               else trident flight model, speed 2.5, adds own motion
 *     other (including a crossbow being loaded): none
 *   else for the main hand, then the off hand:
 *     a loaded crossbow: charged contents decide the shot
 *        firework rocket: rocket model, speed 1.6, fuse from the rocket's flight level
 *        arrows:          arrow model, speed 3.15
 *        more than one projectile (Multishot): side angles are listed, the centre one is the main shot
 *        a crossbow never adds the shooter's own motion
 *   none otherwise
 * </pre>
 */
public final class HeldWeapon {
	/** Reads the loaded projectiles of a crossbow; the component's accessors differ between game versions. */
	private static Function<ChargedProjectiles, List<ItemStack>> projectileReader = charged -> List.of();

	private HeldWeapon() {
	}

	/** @param reader per-version way to get the loaded projectiles out of a crossbow's component */
	public static void setProjectileReader(Function<ChargedProjectiles, List<ItemStack>> reader) {
		projectileReader = reader;
	}

	/** The shot the player would make if they released now, or null if they are not aiming anything. */
	public static @Nullable ShotProfile current(LocalPlayer player, float partial) {
		if (player.isUsingItem()) {
			ItemStack use = player.getUseItem();
			Item item = use.getItem();
			if (item instanceof BowItem) {
				float power = ArrowPhysics.powerForTime(player.getTicksUsingItem() + partial);
				if (power < ArrowPhysics.MIN_POWER) {
					return null;
				}
				return new ShotProfile(FlightModel.ARROW, power * ArrowPhysics.SPEED_PER_POWER, true,
						com.khaopun.kestrel.KestrelSettings.TRAJECTORY_MAX_TICKS, List.of(),
						power >= ArrowPhysics.FULL_CHARGE_POWER);
			}
			if (item instanceof TridentItem) {
				if (EnchantmentHelper.getTridentSpinAttackStrength(use, player) > 0.0F
						|| player.getTicksUsingItem() < WeaponPhysics.TRIDENT_MIN_TICKS) {
					return null;
				}
				return new ShotProfile(FlightModel.TRIDENT, WeaponPhysics.TRIDENT_SPEED, true,
						com.khaopun.kestrel.KestrelSettings.TRAJECTORY_MAX_TICKS, List.of(), true);
			}
			return null;
		}
		ShotProfile crossbow = crossbowShot(player.getMainHandItem());
		return crossbow != null ? crossbow : crossbowShot(player.getOffhandItem());
	}

	/** True if the player holds something this mod can aim (used to keep target tracking running). */
	public static boolean holdingAny(Player player) {
		return isWeapon(player.getMainHandItem()) || isWeapon(player.getOffhandItem());
	}

	private static boolean isWeapon(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;
	}

	private static @Nullable ShotProfile crossbowShot(ItemStack stack) {
		if (!(stack.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(stack)) {
			return null;
		}
		ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
		if (charged == null || charged.isEmpty()) {
			return null;
		}
		List<ItemStack> loaded = projectileReader.apply(charged);
		List<Double> spread = WeaponPhysics.spreadAngles(loaded.size());
		ItemStack rocket = loaded.stream().filter(s -> s.is(Items.FIREWORK_ROCKET)).findFirst().orElse(null);
		if (rocket != null) {
			Fireworks fireworks = rocket.get(DataComponents.FIREWORKS);
			int level = fireworks == null ? 1 : fireworks.flightDuration();
			int ticks = WeaponPhysics.ROCKET_TICKS_PER_LEVEL * (1 + level) + WeaponPhysics.ROCKET_MIDDLE_EXTRA_TICKS;
			return new ShotProfile(FlightModel.ROCKET, WeaponPhysics.CROSSBOW_ROCKET_SPEED, false, ticks, spread, true);
		}
		return new ShotProfile(FlightModel.ARROW, WeaponPhysics.CROSSBOW_ARROW_SPEED, false,
				com.khaopun.kestrel.KestrelSettings.TRAJECTORY_MAX_TICKS, spread, true);
	}
}
