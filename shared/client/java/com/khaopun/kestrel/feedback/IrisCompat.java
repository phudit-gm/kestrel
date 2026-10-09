package com.khaopun.kestrel.feedback;

import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;

/** Detects an active Iris shader pack by reflection, so Kestrel needs no Iris dependency. */
final class IrisCompat {
	private static final Object API;
	private static final Method IN_USE;

	static {
		Object api = null;
		Method method = null;
		if (FabricLoader.getInstance().isModLoaded("iris")) {
			try {
				Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				api = type.getMethod("getInstance").invoke(null);
				method = type.getMethod("isShaderPackInUse");
			} catch (ReflectiveOperationException | LinkageError e) {
				api = null;
				method = null;
			}
		}
		API = api;
		IN_USE = method;
	}

	private IrisCompat() {
	}

	static boolean shaderPackInUse() {
		if (API == null) {
			return false;
		}
		try {
			return (Boolean) IN_USE.invoke(API);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}
}
