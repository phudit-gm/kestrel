package com.khaopun.kestrel.mixin;

import com.khaopun.kestrel.sight.ModeB;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets Mode B swallow the mouse's camera turning. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void kestrel$turn(double yRot, double xRot, CallbackInfo ci) {
		if (ModeB.consumeTurn((Entity) (Object) this, yRot, xRot)) {
			ci.cancel();
		}
	}
}
