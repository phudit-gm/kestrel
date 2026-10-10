package com.khaopun.kestrel.sight;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.khaopun.kestrel.core.MotionClass;
import com.khaopun.kestrel.core.Prediction;
import com.khaopun.kestrel.core.TargetMotion;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;

/**
 * Measurement aid: for every arrow the player shoots at a predicted target, logs where the prediction put the
 * target and where the target really was when the arrow landed, split into "along its walking direction" and
 * "across". Lines start with {@code [kestrel-shot]} in latest.log.
 */
public final class DebugShots {
	private static final Logger LOGGER = LogUtils.getLogger();

	private record Snapshot(int targetId, Vec3 predicted, Vec3 targetAtLaunch, Vec3 targetVelocity, Prediction.Kind kind,
			MotionClass motionClass, double distance, int launchTick) {
	}

	private static final class Flight {
		final Snapshot snapshot;
		Vec3 lastPos;
		int age;
		int still;

		Flight(Snapshot snapshot, Vec3 pos) {
			this.snapshot = snapshot;
			this.lastPos = pos;
		}
	}

	private static Snapshot latest;
	private static long latestTick = Long.MIN_VALUE;
	private static final Map<Integer, Flight> FLIGHTS = new HashMap<>();
	private static long tick;

	private DebugShots() {
	}

	/** Called every frame a prediction exists for the main target. */
	public static void recordPrediction(LocalPlayer player, Entity target, Prediction prediction) {
		if (!com.khaopun.kestrel.KestrelSettings.DEBUG_SHOT_LOG) {
			return;
		}
		TargetMotion motion = LockController.motion();
		Vec3 velocity = motion.isEmpty() ? Vec3.ZERO : motion.meanVelocity();
		latest = new Snapshot(target.getId(), prediction.aim(), target.position(), velocity, prediction.kind(),
				prediction.motion(), player.position().distanceTo(target.position()), (int) tick);
		latestTick = tick;
	}

	public static void onTick(Minecraft mc) {
		if (!com.khaopun.kestrel.KestrelSettings.DEBUG_SHOT_LOG) {
			FLIGHTS.clear();
			return;
		}
		tick++;
		if (mc.level == null || mc.player == null) {
			FLIGHTS.clear();
			return;
		}
		for (Entity e : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(120.0D),
				x -> x instanceof Projectile)) {
			if (e instanceof Projectile projectile && projectile.getOwner() == mc.player) {
				Flight flight = FLIGHTS.get(e.getId());
				if (flight == null) {
					if (latest != null && tick - latestTick <= 3) {
						FLIGHTS.put(e.getId(), new Flight(latest, e.position()));
					}
				} else {
					flight.still = e.position().distanceToSqr(flight.lastPos) < 1.0E-4D ? flight.still + 1 : 0;
					flight.lastPos = e.position();
					flight.age++;
				}
			}
		}
		for (Iterator<Map.Entry<Integer, Flight>> it = FLIGHTS.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, Flight> entry = it.next();
			Entity arrow = mc.level.getEntity(entry.getKey());
			Flight flight = entry.getValue();
			boolean landed = flight.age >= 2 && (arrow == null || arrow.isRemoved() || flight.still >= 2);
			if (arrow == null && flight.age < 2) {
				it.remove();
				continue;
			}
			if (landed) {
				report(mc, entry.getValue());
				it.remove();
			}
		}
	}

	private static void report(Minecraft mc, Flight flight) {
		Snapshot s = flight.snapshot;
		Entity target = mc.level.getEntity(s.targetId());
		if (target == null) {
			LOGGER.info("[kestrel-shot] target gone; class={} dist={} age={}", s.motionClass(), fmt(s.distance()), flight.age);
			return;
		}
		Vec3 actual = target.position();
		Vec3 serverPos = actual;
		if (mc.getSingleplayerServer() != null) {
			net.minecraft.server.level.ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
			Entity serverTarget = serverLevel == null ? null : serverLevel.getEntity(s.targetId());
			if (serverTarget != null) {
				serverPos = serverTarget.position();
			}
		}
		Vec3 error = actual.subtract(s.predicted()); // where the target really is minus where we aimed
		Vec3 dir = new Vec3(s.targetVelocity().x, 0.0D, s.targetVelocity().z);
		double speed = dir.length();
		double along = 0.0D;
		double across = 0.0D;
		if (speed > 1.0E-6D) {
			Vec3 unit = dir.scale(1.0D / speed);
			along = error.x * unit.x + error.z * unit.z;
			across = error.x * -unit.z + error.z * unit.x;
		}
		double serverAlong = 0.0D;
		double lagTicks = 0.0D;
		if (speed > 1.0E-6D) {
			Vec3 unit = dir.scale(1.0D / speed);
			Vec3 serverError = serverPos.subtract(s.predicted());
			serverAlong = serverError.x * unit.x + serverError.z * unit.z;
			Vec3 lag = serverPos.subtract(actual);
			lagTicks = (lag.x * unit.x + lag.z * unit.z) / speed;
		}
		double predictedLead = speed > 1.0E-6D ? s.predicted().subtract(s.targetAtLaunch()).horizontalDistance() : 0.0D;
		double realMove = actual.subtract(s.targetAtLaunch()).horizontalDistance();
		double arrowToTarget = flight.lastPos.distanceTo(actual.add(0.0D, target.getBbHeight() / 2.0D, 0.0D));
		LOGGER.info("[kestrel-shot] class={} kind={} dist={} flightTicks={} speed={}/tick predictedLead={} realMove={} "
				+ "errAlong={} (+ = target went further than aimed) serverErrAlong={} serverLagTicks={} errAcross={} errY={} arrowToTarget={}",
				s.motionClass(), s.kind(), fmt(s.distance()), flight.age, fmt(speed), fmt(predictedLead), fmt(realMove),
				fmt(along), fmt(serverAlong), fmt(lagTicks), fmt(across), fmt(error.y), fmt(arrowToTarget));
	}

	private static String fmt(double v) {
		return String.format("%.2f", v);
	}
}
