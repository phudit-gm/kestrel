package com.khaopun.kestrel.core;

import java.util.List;

/**
 * Everything the physics needs to know about the shot the player is about to make.
 *
 * @param model             how the projectile flies
 * @param speed             launch speed in blocks per tick
 * @param addsShooterMotion true if the shooter's own movement is added to the projectile (bow, trident; not crossbow)
 * @param maxTicks          longest flight worth simulating (a rocket explodes after its fuse)
 * @param extraAngles       degrees to rotate the aim around the up axis for the side projectiles (Multishot), else empty
 * @param fullyCharged      true when drawing any longer would not change the shot
 */
public record ShotProfile(FlightModel model, double speed, boolean addsShooterMotion, int maxTicks,
		List<Double> extraAngles, boolean fullyCharged) {
}
