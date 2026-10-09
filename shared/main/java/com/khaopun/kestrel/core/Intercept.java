package com.khaopun.kestrel.core;

import net.minecraft.world.phys.Vec3;

/** Where a moving target and an arrow meet, and how many ticks the arrow flies to get there. */
public record Intercept(Vec3 point, double flightTicks) {
}
