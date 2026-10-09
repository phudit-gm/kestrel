package com.khaopun.kestrel.core;

import java.util.ArrayDeque;
import java.util.Deque;

import com.khaopun.kestrel.KestrelSettings;

import net.minecraft.world.phys.Vec3;

/**
 * Recent position history of one target and the classification of its movement. Feed it one position per client
 * tick with {@link #record}. Only horizontal movement is used: ground mobs and players keep their height, and a
 * jump or step should not turn a steady walk into "erratic". No client classes, so the server can reuse it.
 *
 * <p>See {@code brief/step2-pseudocode.md} for the classification rule.
 */
public final class TargetMotion {
	private final Deque<Vec3> positions = new ArrayDeque<>();

	public void record(Vec3 position) {
		positions.addLast(position);
		while (positions.size() > KestrelSettings.TARGET_HISTORY_TICKS + 1) {
			positions.removeFirst();
		}
	}

	public void clear() {
		positions.clear();
	}

	public boolean isEmpty() {
		return positions.isEmpty();
	}

	/** Most recent recorded position. Only valid when not empty. */
	public Vec3 latest() {
		return positions.getLast();
	}

	/** Mean of the recorded positions. */
	public Vec3 meanPosition() {
		double x = 0.0D;
		double y = 0.0D;
		double z = 0.0D;
		for (Vec3 p : positions) {
			x += p.x;
			y += p.y;
			z += p.z;
		}
		int n = Math.max(1, positions.size());
		return new Vec3(x / n, y / n, z / n);
	}

	/** Average horizontal velocity in blocks per tick (y = 0). */
	public Vec3 meanVelocity() {
		Vec3[] v = velocities();
		if (v.length == 0) {
			return Vec3.ZERO;
		}
		double x = 0.0D;
		double z = 0.0D;
		for (Vec3 vel : v) {
			x += vel.x;
			z += vel.z;
		}
		return new Vec3(x / v.length, 0.0D, z / v.length);
	}

	public MotionClass classify() {
		Vec3[] v = velocities();
		if (v.length < KestrelSettings.MOTION_MIN_SAMPLES) {
			return MotionClass.STATIONARY;
		}
		double meanSpeed = 0.0D;
		for (Vec3 vel : v) {
			meanSpeed += vel.length();
		}
		meanSpeed /= v.length;
		if (meanSpeed < KestrelSettings.STATIONARY_SPEED) {
			return MotionClass.STATIONARY;
		}
		Vec3 mean = meanVelocity();
		double deviation = 0.0D;
		for (Vec3 vel : v) {
			deviation += vel.subtract(mean).length();
		}
		double variation = (deviation / v.length) / meanSpeed;
		return variation <= KestrelSettings.CONSTANT_MAX_VARIATION ? MotionClass.CONSTANT_VELOCITY : MotionClass.ERRATIC;
	}

	private Vec3[] velocities() {
		if (positions.size() < 2) {
			return new Vec3[0];
		}
		Vec3[] p = positions.toArray(new Vec3[0]);
		Vec3[] v = new Vec3[p.length - 1];
		for (int i = 1; i < p.length; i++) {
			v[i - 1] = new Vec3(p[i].x - p[i - 1].x, 0.0D, p[i].z - p[i - 1].z);
		}
		return v;
	}
}
