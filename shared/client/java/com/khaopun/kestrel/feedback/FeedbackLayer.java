package com.khaopun.kestrel.feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.khaopun.kestrel.core.Prediction;
import com.khaopun.kestrel.feedback.theme.ColorSpec;
import com.khaopun.kestrel.feedback.theme.Easing;
import com.khaopun.kestrel.feedback.theme.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Turns {@link FeedbackEvent}s into what the player sees and hears, entirely from the active {@link Theme}.
 * Every colour, size, duration and sound id comes from the theme; this class contains none.
 *
 * <p>Events are level-triggered per frame. This class keeps one animated {@link Track} per element, moves it toward
 * 1 while its event is active and toward 0 otherwise, and draws whatever has a non-zero value, so elements fade out
 * after their event stops. Drawing uses the game's gizmo system and must happen in a render callback where the
 * per-frame gizmo collector is active ({@link FeedbackBus#frameEnd()} is called from there).
 */
public final class FeedbackLayer implements FeedbackListener {
	private static final FeedbackLayer INSTANCE = new FeedbackLayer();
	private static final double EPS = 0.004D;
	private static final double MAX_DT = 0.1D;
	private static final long START_NANOS = System.nanoTime();

	public static void init() {
		FeedbackBus.register(INSTANCE);
	}

	static void onThemeReloaded() {
		INSTANCE.soundCache.clear();
		INSTANCE.particleCache.clear();
		INSTANCE.tokens = -1.0D;
	}

	/** A 0..1 value that eases toward its target over a duration. */
	private static final class Track {
		double value;
		boolean target;

		void update(boolean wanted, double dt, double riseSeconds, double fallSeconds, boolean animate) {
			boolean was = target;
			target = wanted;
			double duration = wanted ? riseSeconds : fallSeconds;
			if (!animate || duration <= 0.0D) {
				value = wanted ? 1.0D : 0.0D;
			} else {
				value = wanted ? Math.min(1.0D, value + dt / duration) : Math.max(0.0D, value - dt / duration);
			}
			entered = wanted && !was;
		}

		boolean entered;

		boolean visible() {
			return value > EPS;
		}
	}

	private static final class CandidateState {
		final Track appear = new Track();
		final Track hit = new Track();
		AABB box;
	}

	// --- inputs collected during one frame ---
	private SightFrame frameIn;
	private boolean impactIn;
	private boolean fullIn;
	private final Map<Integer, Candidate> candidatesIn = new HashMap<>();
	private final Map<Integer, Candidate> hitsIn = new HashMap<>();
	private LockInfo lockIn;
	private GhostInfo ghostIn;
	private boolean turningIn;
	private final List<GhostInfo> othersIn = new ArrayList<>();
	private final List<UnlockInfo> unlocksIn = new ArrayList<>();

	// --- persistent state ---
	private final Track trajectory = new Track();
	private final Track impact = new Track();
	private final Track full = new Track();
	private final Map<Integer, CandidateState> candidates = new HashMap<>();
	private SightFrame lastFrame;
	private final Track lock = new Track();
	private final Track ghost = new Track();
	private final Track aligned = new Track();
	private LockInfo lastLock;
	private GhostInfo lastGhost;
	/** Boxes of released locks, shown in red for a while: {box, startSeconds}. */
	private final List<Object[]> unlockFx = new ArrayList<>();
	private long lastNanos = System.nanoTime();

	private final Map<String, SoundEvent> soundCache = new HashMap<>();
	private final Map<String, ParticleOptions> particleCache = new HashMap<>();
	private double tokens = -1.0D;
	private double lastEmitSeconds;

	private FeedbackLayer() {
	}

	@Override
	public void onEvent(FeedbackEvent event, Object payload) {
		switch (event) {
			case TRAJECTORY_VISIBLE -> frameIn = (SightFrame) payload;
			case IMPACT_POINT -> impactIn = true;
			case FULL_CHARGE -> fullIn = true;
			case CANDIDATE_IN_RANGE -> {
				Candidate c = (Candidate) payload;
				candidatesIn.put(c.id(), c);
			}
			case TARGET_UNDER_TRAJECTORY -> {
				Candidate c = (Candidate) payload;
				hitsIn.put(c.id(), c);
			}
			case TARGET_LOCKED -> lockIn = (LockInfo) payload;
			case TARGET_UNLOCKED, TARGET_LOST -> unlocksIn.add((UnlockInfo) payload);
			case GHOST_PREDICTED -> ghostIn = (GhostInfo) payload;
			case AIM_TURNING -> turningIn = true;
			case GHOST_OTHER -> othersIn.add((GhostInfo) payload);
			default -> {
				// AIM_ALIGNED, AIM_MOVING and TARGET_UNREACHABLE carry the same GhostInfo as GHOST_PREDICTED, whose
				// flags the layer reads directly; no separate handling needed.
			}
		}
	}

	@Override
	public void onFrameEnd() {
		Theme th = ThemeStore.theme();
		long nowNanos = System.nanoTime();
		double dt = Math.min(MAX_DT, (nowNanos - lastNanos) / 1.0e9D);
		lastNanos = nowNanos;
		double seconds = (nowNanos - START_NANOS) / 1.0e9D;

		boolean animate = th.bool("animation.enabled");
		double speed = Math.max(0.01D, th.num("animation.speed"));
		double fadeIn = th.num("animation.fadeIn") / speed;
		double fadeOut = th.num("animation.fadeOut") / speed;
		double hitDuration = th.num("animation.hitDuration") / speed;
		double fullDuration = th.num("animation.fullDuration") / speed;

		if (frameIn != null) {
			lastFrame = frameIn;
		}
		trajectory.update(frameIn != null && th.enabled("trajectory"), dt, fadeIn, fadeOut, animate);
		impact.update(frameIn != null && impactIn && th.enabled("impact"), dt, fadeIn, fadeOut, animate);
		full.update(frameIn != null && fullIn && th.enabled("fullCharge"), dt, fullDuration, fadeOut, animate);

		if (full.entered) {
			playSound(th, "fullCharge");
			if (lastFrame != null) {
				burst(th, "fullCharge", crosshairPoint(th, lastFrame, 0), seconds);
			}
		}

		updateCandidates(th, dt, fadeIn, fadeOut, hitDuration, animate, seconds);
		updateLock(th, dt, fadeIn, fadeOut, hitDuration, animate, seconds);

		if (lastFrame != null) {
			if (trajectory.visible()) {
				drawTrajectory(th, lastFrame, ease(th, "animation.easing", trajectory.value), easedFull(th), seconds);
			}
			if (impact.visible()) {
				drawImpact(th, lastFrame, ease(th, "animation.easing", impact.value), seconds);
			}
		}
		drawCandidates(th, seconds);
		drawLock(th, seconds);
		drawGhost(th, seconds);
		drawOtherGhosts(th, seconds);
		drawUnlockFx(th, seconds);
		drawCrosshairLabels(th, seconds);

		frameIn = null;
		impactIn = false;
		fullIn = false;
		candidatesIn.clear();
		hitsIn.clear();
		lockIn = null;
		ghostIn = null;
		turningIn = false;
		othersIn.clear();
		unlocksIn.clear();
	}

	// ---------------------------------------------------------------- state updates

	private void updateCandidates(Theme th, double dt, double fadeIn, double fadeOut, double hitDuration, boolean animate,
			double seconds) {
		for (Candidate c : candidatesIn.values()) {
			CandidateState state = candidates.computeIfAbsent(c.id(), k -> new CandidateState());
			state.box = c.box();
		}
		boolean candidatesOn = th.enabled("candidates");
		boolean hitOn = th.enabled("hit");
		for (Iterator<Map.Entry<Integer, CandidateState>> it = candidates.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Integer, CandidateState> entry = it.next();
			CandidateState state = entry.getValue();
			boolean seen = candidatesIn.containsKey(entry.getKey());
			boolean hit = hitsIn.containsKey(entry.getKey());
			state.appear.update(seen && candidatesOn, dt, fadeIn, fadeOut, animate);
			state.hit.update(seen && hit && hitOn, dt, hitDuration, hitDuration, animate);
			if (state.hit.entered && state.box != null) {
				burst(th, "hit", new Vec3((state.box.minX + state.box.maxX) / 2.0D, state.box.maxY,
						(state.box.minZ + state.box.maxZ) / 2.0D), seconds);
			}
			if (!seen && !state.appear.visible() && !state.hit.visible()) {
				it.remove();
			}
		}
	}

	// ---------------------------------------------------------------- trajectory and impact

	private void drawTrajectory(Theme th, SightFrame frame, double alpha, double fullMix, double seconds) {
		List<Vec3> raw = frame.points();
		int n = raw.size();
		if (n < 2) {
			return;
		}
		Vec3[] p = new Vec3[n];
		double[] cum = new double[n];
		p[0] = handShifted(th, raw.get(0), 0, frame.side());
		for (int i = 1; i < n; i++) {
			// The last point is the real impact: never shifted.
			p[i] = i == n - 1 ? raw.get(i) : handShifted(th, raw.get(i), i, frame.side());
			cum[i] = cum[i - 1] + p[i].distanceTo(p[i - 1]);
		}
		double total = Math.max(cum[n - 1], 1.0E-6D);
		ColorSpec normal = th.color("trajectory.color");
		ColorSpec charged = th.color("trajectory.colorFull");
		double floor = th.num("minAlpha");
		String style = th.str("trajectory.style");
		float width = (float) th.num("trajectory.width");

		switch (style) {
			case "line", "dashed" -> {
				double dash = Math.max(0.05D, th.num("trajectory.dashLength"));
				for (int i = 1; i < n; i++) {
					double mid = (cum[i] + cum[i - 1]) / 2.0D;
					if (style.equals("dashed") && ((long) Math.floor(mid / dash) & 1L) == 1L) {
						continue;
					}
					double t = mid / total;
					int color = trajectoryColor(th, normal, charged, fullMix, t, seconds, floor, alpha,
							p[i].lerp(p[i - 1], 0.5D), frame.eye());
					Gizmos.line(p[i - 1], p[i], color, width);
				}
			}
			default -> {
				double spacing = Math.max(0.05D, th.num("trajectory.spacing"));
				double size = th.num("trajectory.beadSize");
				double taper = th.num("trajectory.taper");
				int seg = 1;
				for (double d = 0.0D; d <= total; d += spacing) {
					while (seg < n - 1 && cum[seg] < d) {
						seg++;
					}
					double segLen = cum[seg] - cum[seg - 1];
					double f = segLen > 0.0D ? (d - cum[seg - 1]) / segLen : 0.0D;
					Vec3 pos = p[seg - 1].lerp(p[seg], f);
					double t = d / total;
					int color = trajectoryColor(th, normal, charged, fullMix, t, seconds, floor, alpha, pos, frame.eye());
					double bead = size * (1.0D - taper + taper * Math.sin(Math.PI * t));
					point(th, pos, color, (float) Math.max(1.0D, bead));
				}
			}
		}
	}

	private int trajectoryColor(Theme th, ColorSpec normal, ColorSpec charged, double fullMix, double t, double seconds,
			double floor, double alpha, Vec3 pos, Vec3 eye) {
		int color = ColorSpec.lerp(normal.at(t, seconds), charged.at(t, seconds), fullMix);
		color = ColorSpec.withMinAlpha(color, floor);
		return ColorSpec.withAlpha(color, alpha * distanceFade(th, pos, eye));
	}

	/** Visual only: shifts the first part of the line toward the bow hand so the arc reads from behind. */
	private Vec3 handShifted(Theme th, Vec3 point, int index, Vec3 side) {
		double fadeTicks = th.num("trajectory.handFadeTicks");
		double weight = fadeTicks <= 0.0D ? 0.0D : 1.0D - index / fadeTicks;
		if (weight <= 0.0D) {
			return point;
		}
		return point.add(side.scale(th.num("trajectory.handOffsetSide") * weight))
				.subtract(0.0D, th.num("trajectory.handOffsetDown") * weight, 0.0D);
	}

	private void drawImpact(Theme th, SightFrame frame, double alpha, double seconds) {
		Vec3 c = frame.impact();
		double h = th.num("impact.size");
		double floor = th.num("minAlpha");
		int core = fade(th, ColorSpec.withMinAlpha(th.color("impact.color").at(0.5D, seconds), floor), alpha, c, frame.eye());
		float width = (float) th.num("impact.width");
		switch (th.str("impact.style")) {
			case "cross" -> {
				Gizmos.line(c.add(-h, 0, 0), c.add(h, 0, 0), core, width);
				Gizmos.line(c.add(0, -h, 0), c.add(0, h, 0), core, width);
				Gizmos.line(c.add(0, 0, -h), c.add(0, 0, h), core, width);
			}
			case "pixelCross" -> {
				float size = (float) th.num("impact.pixelSize");
				point(th, c, core, size);
				for (int axis = 0; axis < 3; axis++) {
					for (int sign = -1; sign <= 1; sign += 2) {
						for (int step = 1; step <= 2; step++) {
							double d = sign * h * step / 2.0D;
							point(th, c.add(axis == 0 ? d : 0, axis == 1 ? d : 0, axis == 2 ? d : 0), core, size);
						}
					}
				}
			}
			default -> Gizmos.cuboid(new AABB(c.x - h, c.y - h, c.z - h, c.x + h, c.y + h, c.z + h),
					GizmoStyle.stroke(core, width));
		}
		double edgeWidth = th.num("impact.edgeWidth");
		if (edgeWidth > 0.0D && !th.str("impact.style").equals("box")) {
			int edge = fade(th, ColorSpec.withMinAlpha(th.color("impact.edgeColor").at(0.5D, seconds), floor), alpha, c, frame.eye());
			Gizmos.cuboid(new AABB(c.x - h, c.y - h, c.z - h, c.x + h, c.y + h, c.z + h),
					GizmoStyle.stroke(edge, (float) edgeWidth));
		}
	}

	// ---------------------------------------------------------------- candidate frames

	private void drawCandidates(Theme th, double seconds) {
		if (candidates.isEmpty() || lastFrame == null) {
			return;
		}
		Vec3 eye = lastFrame.eye();
		double floor = th.num("minAlpha");
		for (CandidateState state : candidates.values()) {
			if (state.box == null || !(state.appear.visible() || state.hit.visible())) {
				continue;
			}
			double alpha = ease(th, "animation.easing", Math.max(state.appear.value, state.hit.value));
			double k = Easing.apply(th.str("animation.hitEasing"), state.hit.value);
			double kc = Math.max(0.0D, Math.min(1.0D, state.hit.value));
			drawFrame(th, state.box, "candidate", "hit", k, kc, alpha, floor, seconds, eye);
			if (state.hit.visible()) {
				drawTargetLabel(th, state.box, ease(th, "animation.easing", state.hit.value), seconds);
			}
		}
	}

	/** Draws one entity frame, blending candidate -> hit style by {@code k} (may overshoot for a bounce). */
	private void drawFrame(Theme th, AABB base, String fromKey, String toKey, double k, double kc, double alpha, double floor,
			double seconds, Vec3 eye) {
		double inflate = lerp(th.num(fromKey + ".inflate"), th.num(toKey + ".inflate"), k);
		float width = (float) lerp(th.num(fromKey + ".width"), th.num(toKey + ".width"), k);
		String which = kc > 0.5D ? toKey : fromKey;
		String frameStyle = th.str(which + ".frame");
		ColorSpec from = th.color(fromKey + ".color");
		ColorSpec to = th.color(toKey + ".color");
		AABB box = base.inflate(inflate);
		Vec3 center = box.getCenter();

		if (frameStyle.equals("box")) {
			int color = frameColor(th, from, to, kc, 0.5D, seconds, floor, alpha, center, eye);
			Gizmos.cuboid(box, GizmoStyle.stroke(color, width));
			return;
		}
		double arm = lerp(th.num(fromKey + ".arm"), th.num(toKey + ".arm"), k);
		boolean pixels = frameStyle.equals("pixelCorners");
		double pixel = Math.max(0.02D, th.num(which + ".pixel"));
		float pixelSize = (float) th.num(which + ".pixelSize");
		boolean round = th.bool(which + ".roundCorners");
		double[] min = {box.minX, box.minY, box.minZ};
		double[] max = {box.maxX, box.maxY, box.maxZ};
		for (int corner = 0; corner < 8; corner++) {
			double[] origin = new double[3];
			for (int axis = 0; axis < 3; axis++) {
				origin[axis] = ((corner >> axis) & 1) == 0 ? min[axis] : max[axis];
			}
			for (int axis = 0; axis < 3; axis++) {
				double dir = ((corner >> axis) & 1) == 0 ? 1.0D : -1.0D;
				double length = (max[axis] - min[axis]) * arm;
				if (pixels) {
					int steps = Math.max(1, (int) Math.round(length / pixel));
					for (int s = round ? 1 : 0; s <= steps; s++) {
						double t = (double) s / steps;
						Vec3 pos = pointAlong(origin, axis, dir * length * t);
						point(th, pos, frameColor(th, from, to, kc, t, seconds, floor, alpha, pos, eye), pixelSize);
					}
				} else {
					Vec3 a = pointAlong(origin, axis, 0.0D);
					Vec3 mid = pointAlong(origin, axis, dir * length / 2.0D);
					Vec3 b = pointAlong(origin, axis, dir * length);
					Gizmos.line(a, mid, frameColor(th, from, to, kc, 0.25D, seconds, floor, alpha, mid, eye), width);
					Gizmos.line(mid, b, frameColor(th, from, to, kc, 0.75D, seconds, floor, alpha, mid, eye), width);
				}
			}
		}
	}

	private static Vec3 pointAlong(double[] origin, int axis, double offset) {
		return new Vec3(origin[0] + (axis == 0 ? offset : 0.0D), origin[1] + (axis == 1 ? offset : 0.0D),
				origin[2] + (axis == 2 ? offset : 0.0D));
	}

	private int frameColor(Theme th, ColorSpec from, ColorSpec to, double kc, double t, double seconds, double floor,
			double alpha, Vec3 pos, Vec3 eye) {
		int color = ColorSpec.lerp(from.at(t, seconds), to.at(t, seconds), kc);
		return fade(th, ColorSpec.withMinAlpha(color, floor), alpha, pos, eye);
	}

	// ---------------------------------------------------------------- labels

	private void drawTargetLabel(Theme th, AABB box, double alpha, double seconds) {
		Vec3 pos = new Vec3((box.minX + box.maxX) / 2.0D, box.maxY + th.num("label.heightAbove"), (box.minZ + box.maxZ) / 2.0D);
		text(th.str("label.text"), pos, th.color("label.color").at(0.5D, seconds), alpha, th.num("label.scale"));
	}

	private void drawCrosshairLabels(Theme th, double seconds) {
		if (lastFrame == null) {
			return;
		}
		if (full.visible()) {
			double eased = Easing.apply(th.str("animation.fullEasing"), full.value);
			double pop = lerp(1.0D - th.num("fullCharge.pulse"), 1.0D, eased);
			text(th.str("fullCharge.text"), crosshairPoint(th, lastFrame, 0), th.color("fullCharge.color").at(0.5D, seconds),
					Math.min(1.0D, full.value), th.num("fullCharge.scale") * pop);
		}
		double hit = 0.0D;
		for (CandidateState state : candidates.values()) {
			hit = Math.max(hit, state.hit.value);
		}
		// A second TARGET label near the crosshair: the one above the head is easy to lose off-screen.
		if (hit > EPS) {
			text(th.str("label.text"), crosshairPoint(th, lastFrame, 1), th.color("label.color").at(0.5D, seconds),
					ease(th, "animation.easing", hit), th.num("label.scale"));
		}
	}

	/** A point floating in front of the camera, just below the crosshair; row 0 is the first line, 1 the next. */
	private Vec3 crosshairPoint(Theme th, SightFrame frame, int row) {
		return frame.eye().add(frame.look().scale(th.num("crosshairLabels.distance")))
				.subtract(0.0D, th.num("crosshairLabels.drop") + row * th.num("crosshairLabels.rowGap"), 0.0D);
	}

	private void text(String text, Vec3 pos, int argb, double alpha, double scale) {
		int color = ColorSpec.withAlpha(argb, alpha);
		// The game draws text with an alpha of 3 or less as fully opaque, so skip nearly invisible text.
		if (((color >>> 24) & 0xFF) <= 8) {
			return;
		}
		Gizmos.billboardText(text, pos,
				TextGizmo.Style.forColorAndCentered(color).withScale((float) (TextGizmo.Style.DEFAULT_SCALE * scale)));
	}

	// ---------------------------------------------------------------- lock, ghost and release effects

	private void updateLock(Theme th, double dt, double fadeIn, double fadeOut, double hitDuration, boolean animate,
			double seconds) {
		boolean lockOn = th.enabled("lock");
		if (lockIn != null) {
			lastLock = lockIn;
		}
		lock.update(lockIn != null && lockOn, dt, fadeIn, fadeOut, animate);
		if (lock.entered) {
			playSound(th, "locked");
		}
		if (ghostIn != null) {
			lastGhost = ghostIn;
		}
		ghost.update(ghostIn != null && th.enabled("ghost"), dt, fadeIn, fadeOut, animate);
		aligned.update(ghostIn != null && ghostIn.aligned(), dt, hitDuration, hitDuration, animate);
		if (lockOn) {
			for (UnlockInfo info : unlocksIn) {
				unlockFx.add(new Object[] {info.box(), seconds});
				playSound(th, "unlocked");
			}
		}
	}

	private void drawLock(Theme th, double seconds) {
		if (!lock.visible() || lastLock == null) {
			return;
		}
		double alpha = ease(th, "animation.easing", lock.value);
		if (turningIn) {
			// Mode B: the box flashes while the camera is still turning.
			alpha *= 0.5D + 0.5D * Math.sin(seconds * th.num("locked.turningFlashHz") * 2.0D * Math.PI);
		}
		double k = Easing.apply(th.str("animation.hitEasing"), aligned.value);
		Vec3 eye = lastFrame != null ? lastFrame.eye() : lastLock.box().getCenter();
		drawFrame(th, lastLock.box(), "locked", "aligned", k, Math.max(0.0D, Math.min(1.0D, aligned.value)), alpha,
				th.num("minAlpha"), seconds, eye);
	}

	private void drawGhost(Theme th, double seconds) {
		if (!ghost.visible() || lastGhost == null) {
			return;
		}
		boolean unreachable = lastGhost.kind() == Prediction.Kind.UNREACHABLE;
		boolean faded = lastGhost.kind() == Prediction.Kind.FADED;
		String fromKey = unreachable ? "unreachable" : "ghost";
		double alpha = ease(th, "animation.easing", ghost.value) * (faded ? th.num("ghost.fadedAlpha") : 1.0D);
		AABB box = lastGhost.ghost();
		if (faded) {
			ThreadLocalRandom rnd = ThreadLocalRandom.current();
			double j = th.num("ghost.jitter");
			box = box.move((rnd.nextDouble() - 0.5D) * 2.0D * j, (rnd.nextDouble() - 0.5D) * j, (rnd.nextDouble() - 0.5D) * 2.0D * j);
		}
		double k = unreachable ? 0.0D : Easing.apply(th.str("animation.hitEasing"), aligned.value);
		double kc = unreachable ? 0.0D : Math.max(0.0D, Math.min(1.0D, aligned.value));
		Vec3 eye = lastFrame != null ? lastFrame.eye() : box.getCenter();
		drawFrame(th, box, fromKey, "aligned", k, kc, alpha, th.num("minAlpha"), seconds, eye);

		double fillAlpha = th.num("ghost.fillAlpha");
		// Filled boxes are not drawn by shader packs, so the fill is only added without one.
		if (fillAlpha > 0.0D && !IrisCompat.shaderPackInUse()) {
			int fill = ColorSpec.lerp(th.color(fromKey + ".color").at(0.5D, seconds), th.color("aligned.color").at(0.5D, seconds), kc);
			Gizmos.cuboid(box, GizmoStyle.fill(ColorSpec.withAlpha(fill, alpha * fillAlpha)));
		}
		if (unreachable) {
			text(th.str("ghost.unreachableText"),
					new Vec3((box.minX + box.maxX) / 2.0D, box.maxY + th.num("ghost.labelHeightAbove"), (box.minZ + box.maxZ) / 2.0D),
					th.color("unreachable.color").at(0.5D, seconds), alpha, th.num("ghost.labelScale"));
		}
	}

	/** Faint ghosts of the unlocked candidates. Stateless: shown only on frames their event arrives. */
	private void drawOtherGhosts(Theme th, double seconds) {
		if (othersIn.isEmpty() || !th.enabled("ghost") || !th.bool("ghost.others")) {
			return;
		}
		double alpha = th.num("ghost.othersAlpha");
		Vec3 eye = lastFrame != null ? lastFrame.eye() : null;
		for (GhostInfo info : othersIn) {
			AABB box = info.ghost();
			double a = alpha * (info.kind() == Prediction.Kind.FADED ? th.num("ghost.fadedAlpha") : 1.0D);
			drawFrame(th, box, "ghost", "aligned", 0.0D, 0.0D, a, th.num("minAlpha"), seconds, eye != null ? eye : box.getCenter());
		}
	}

	private void drawUnlockFx(Theme th, double seconds) {
		double duration = th.num("unlock.duration");
		double fadeFraction = Math.max(0.01D, th.num("unlock.fadeFraction"));
		boolean animate = th.bool("animation.enabled");
		double floor = th.num("minAlpha");
		for (Iterator<Object[]> it = unlockFx.iterator(); it.hasNext();) {
			Object[] fx = it.next();
			double elapsed = seconds - (Double) fx[1];
			if (elapsed >= duration) {
				it.remove();
				continue;
			}
			double alpha = animate ? Math.min(1.0D, (duration - elapsed) / (duration * fadeFraction)) : 1.0D;
			AABB box = (AABB) fx[0];
			Vec3 eye = lastFrame != null ? lastFrame.eye() : box.getCenter();
			drawFrame(th, box, "unlock", "unlock", 0.0D, 0.0D, alpha, floor, seconds, eye);
		}
	}

	// ---------------------------------------------------------------- sound and particles

	private void playSound(Theme th, String key) {
		if (!th.enabled("sounds") || !th.bool("sounds." + key + ".enabled")) {
			return;
		}
		float volume = (float) th.num("sounds." + key + ".volume");
		if (volume <= 0.0F) {
			return;
		}
		String id = th.str("sounds." + key + ".id");
		SoundEvent event = soundCache.computeIfAbsent(id, k -> {
			Identifier identifier = Identifier.tryParse(k);
			return identifier == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(identifier);
		});
		if (event != null) {
			Minecraft.getInstance().getSoundManager()
					.play(SimpleSoundInstance.forUI(event, (float) th.num("sounds." + key + ".pitch"), volume));
		}
	}

	/** Spawns a few client-side particles, within the theme's particle budget. Never touches the server. */
	private void burst(Theme th, String key, Vec3 at, double seconds) {
		if (!th.enabled("particles") || !th.bool("particles." + key + ".enabled")) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		double maxPerSecond = th.num("limits.maxParticles");
		double minInterval = th.num("limits.maxUpdatesPerSecond") > 0.0D ? 1.0D / th.num("limits.maxUpdatesPerSecond") : 0.0D;
		if (tokens < 0.0D) {
			tokens = maxPerSecond;
			lastEmitSeconds = seconds - 1.0D;
		}
		tokens = Math.min(maxPerSecond, tokens + (seconds - lastEmitSeconds) * maxPerSecond);
		if (seconds - lastEmitSeconds < minInterval) {
			return;
		}
		int count = (int) Math.min(th.integer("particles." + key + ".count"), Math.floor(tokens));
		if (count <= 0) {
			return;
		}
		String id = th.str("particles." + key + ".id");
		ParticleOptions options = particleCache.computeIfAbsent(id, k -> {
			Identifier identifier = Identifier.tryParse(k);
			ParticleType<?> type = identifier == null ? null : BuiltInRegistries.PARTICLE_TYPE.getValue(identifier);
			return type instanceof ParticleOptions o ? o : null;
		});
		if (options == null) {
			return;
		}
		tokens -= count;
		lastEmitSeconds = seconds;
		double spread = th.num("particles." + key + ".spread");
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			mc.level.addParticle(options, at.x + (rnd.nextDouble() - 0.5D) * spread, at.y + (rnd.nextDouble() - 0.5D) * spread,
					at.z + (rnd.nextDouble() - 0.5D) * spread, 0.0D, 0.0D, 0.0D);
		}
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * Draws a dot. Shader packs may not draw the game's point primitive, so when a shader pack is active (or the theme
	 * forces it) the dot is drawn as a small 3-axis cross of lines instead, which shader packs do draw.
	 */
	private void point(Theme th, Vec3 pos, int color, float size) {
		String mode = th.str("compat.points");
		boolean asLines = mode.equals("lines") || (mode.equals("auto") && IrisCompat.shaderPackInUse());
		if (!asLines) {
			Gizmos.point(pos, color, size);
			return;
		}
		double h = size * th.num("compat.pointLineScale");
		float width = (float) th.num("compat.pointLineWidth");
		Gizmos.line(pos.add(-h, 0.0D, 0.0D), pos.add(h, 0.0D, 0.0D), color, width);
		Gizmos.line(pos.add(0.0D, -h, 0.0D), pos.add(0.0D, h, 0.0D), color, width);
		Gizmos.line(pos.add(0.0D, 0.0D, -h), pos.add(0.0D, 0.0D, h), color, width);
	}

	private static double easedFull(Theme th) {
		return Math.max(0.0D, Math.min(1.0D, Easing.apply(th.str("animation.fullEasing"), INSTANCE.full.value)));
	}

	private static double ease(Theme th, String path, double value) {
		return Math.max(0.0D, Math.min(1.0D, Easing.apply(th.str(path), value)));
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/** Fades near the camera so effects do not block the view or the crosshair. */
	private static double distanceFade(Theme th, Vec3 pos, Vec3 eye) {
		double start = th.num("distanceFade.start");
		double end = th.num("distanceFade.end");
		if (end <= start) {
			return 1.0D;
		}
		double t = Math.max(0.0D, Math.min(1.0D, (pos.distanceTo(eye) - start) / (end - start)));
		return t * t * (3.0D - 2.0D * t);
	}

	private static int fade(Theme th, int color, double alpha, Vec3 pos, Vec3 eye) {
		return ColorSpec.withAlpha(color, alpha * distanceFade(th, pos, eye));
	}
}
