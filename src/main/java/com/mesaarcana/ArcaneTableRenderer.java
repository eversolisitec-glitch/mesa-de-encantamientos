package com.mesaarcana;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja la mesa de encantamiento normal (vanilla) y encima le anade capas
 * brillantes con texturas propias (assets/mesaarcana/textures/effect).
 * Las texturas son PNG con transparencia: se pueden cambiar con un resource pack.
 */
public class ArcaneTableRenderer extends EnchantTableRenderer {
	private static final int FULL_BRIGHT = 0xF000F0;

	private static final Identifier RING_OUTER = tex("ring_outer");
	private static final Identifier RING_INNER = tex("ring_inner");
	private static final Identifier FLOOR_STATIC = tex("floor_static");
	private static final Identifier FLOOR_RING = tex("floor_ring");
	private static final Identifier GLYPHS = tex("glyphs");
	private static final Identifier BEAM = tex("beam");

	private static final Map<Long, long[]> POWER_CACHE = new HashMap<>();

	private static Identifier tex(String name) {
		return Identifier.fromNamespaceAndPath("mesaarcana", "textures/effect/" + name + ".png");
	}

	public ArcaneTableRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void submit(EnchantTableRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, pose, collector, camera);

		MesaConfig cfg = MesaConfig.get();
		if (!cfg.enabled) {
			return;
		}

		double t = (System.currentTimeMillis() % 7_200_000L) / 1000.0 * cfg.speed;
		double pw = power(state.blockPos) / 15.0;
		float base = (float) Math.min(1.0, (0.55 + 0.45 * pw) * cfg.intensity);
		float pulse = (float) (0.82 + 0.18 * Math.sin(t * 2.0));
		float bob = (float) (0.025 * Math.sin(t * 1.4));
		double spin = 1.0 + pw * 0.8;

		// Suelo: circulo magico (fijo) + anillo de runas que gira
		if (cfg.floor) {
			final float a1 = base * 0.85f;
			final float a2 = base * pulse;
			final float ang = (float) (-t * 0.25 * spin);
			collector.submitCustomGeometry(pose, RenderTypes.eyes(FLOOR_STATIC), (p, c) ->
				flat(c, p, 0.5f, 0.012f, 0.5f, 1.48f, 0f, 0f, 0f, 1f, 1f, color(a1)));
			collector.submitCustomGeometry(pose, RenderTypes.eyes(FLOOR_RING), (p, c) ->
				flat(c, p, 0.5f, 0.014f, 0.5f, 1.2f, ang, 0f, 0f, 1f, 1f, color(a2)));
		}

		// Anillos flotando alrededor del libro
		if (cfg.ring) {
			final float a = base * pulse;
			final float angOuter = (float) (t * 0.4 * spin);
			final float angInner = (float) (-t * 0.65 * spin);
			final float yOuter = 0.98f + bob;
			final float yInner = 1.28f - bob;
			collector.submitCustomGeometry(pose, RenderTypes.eyes(RING_OUTER), (p, c) ->
				flat(c, p, 0.5f, yOuter, 0.5f, 1.25f, angOuter, 0f, 0f, 1f, 1f, color(a)));
			collector.submitCustomGeometry(pose, RenderTypes.eyes(RING_INNER), (p, c) ->
				flat(c, p, 0.5f, yInner, 0.5f, 0.85f, angInner, 0f, 0f, 1f, 1f, color(a * 0.9f)));
		}

		// Haz de luz
		if (cfg.beam) {
			final float a = base * 0.8f * pulse;
			collector.submitCustomGeometry(pose, RenderTypes.eyes(BEAM), (p, c) -> {
				beam(c, p, 0.5f, 1.1f, 3.3f, 0.5f, 0.3f, 0f, color(a));
				beam(c, p, 0.5f, 1.1f, 3.3f, 0.5f, 0.3f, (float) (Math.PI / 2), color(a));
			});
		}

		// Runas sueltas que suben (siempre miran a la camara)
		if (cfg.glyphs) {
			final int n = 3 + (int) Math.round(pw * 6);
			final Vector3f right = camera.orientation.transform(new Vector3f(1, 0, 0));
			final Vector3f up = camera.orientation.transform(new Vector3f(0, 1, 0));
			final double tt = t;
			collector.submitCustomGeometry(pose, RenderTypes.eyes(GLYPHS), (p, c) -> {
				for (int i = 0; i < n; i++) {
					double phase = frac(tt * 0.12 + i / (double) n);
					double h = 0.95 + phase * 1.5;
					double ang = i * 2.4 + tt * 0.5;
					double rad = 0.85 + 0.3 * Math.sin(i * 1.7);
					float x = (float) (0.5 + Math.cos(ang) * rad);
					float z = (float) (0.5 + Math.sin(ang) * rad);
					float alpha = (float) (Math.sin(phase * Math.PI) * base);
					float half = (float) (0.17 + 0.07 * frac(i * 0.37));
					int cell = i % 8;
					float u0 = (cell % 4) * 0.25f;
					float v0 = (cell / 4) * 0.5f;
					board(c, p, x, (float) h, z, right, up, half, u0, v0, u0 + 0.25f, v0 + 0.5f, color(alpha));
				}
			});
		}
	}

	// ------------------------------------------------------------ poder de estanterias
	private static int power(BlockPos pos) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return 0;
		}
		long now = level.getGameTime();
		long key = pos.asLong();
		long[] e = POWER_CACHE.get(key);
		if (e != null && now - e[1] < 20) {
			return (int) e[0];
		}
		int n = 0;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
					continue;
				}
				for (int dy = 0; dy <= 1; dy++) {
					m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					if (level.getBlockState(m).is(Blocks.BOOKSHELF)) {
						n++;
					}
				}
			}
		}
		int power = Math.min(15, n / 2);
		if (POWER_CACHE.size() > 64) {
			POWER_CACHE.clear();
		}
		POWER_CACHE.put(key, new long[] {power, now});
		return power;
	}

	// ------------------------------------------------------------ geometria
	private static double frac(double v) {
		return v - Math.floor(v);
	}

	private static int color(float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
		return (a << 24) | 0xFFFFFF;
	}

	private static void put(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, float u, float v, int col) {
		c.addVertex(p, x, y, z)
			.setColor(col)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(FULL_BRIGHT)
			.setNormal(p, 0f, 1f, 0f);
	}

	/** Cuatro esquinas (0:u0,v0 1:u1,v0 2:u1,v1 3:u0,v1), visible por las dos caras. */
	private static void quad(VertexConsumer c, PoseStack.Pose p, float[] x, float[] y, float[] z,
							 float u0, float v0, float u1, float v1, int col) {
		float[] us = {u0, u1, u1, u0};
		float[] vs = {v0, v0, v1, v1};
		for (int i = 0; i < 4; i++) {
			put(c, p, x[i], y[i], z[i], us[i], vs[i], col);
		}
		for (int i = 3; i >= 0; i--) {
			put(c, p, x[i], y[i], z[i], us[i], vs[i], col);
		}
	}

	/** Cuadrado horizontal girado alrededor del eje Y. */
	private static void flat(VertexConsumer c, PoseStack.Pose p, float cx, float y, float cz, float half, float ang,
							 float u0, float v0, float u1, float v1, int col) {
		float cs = (float) Math.cos(ang);
		float sn = (float) Math.sin(ang);
		float[] lx = {-half, half, half, -half};
		float[] lz = {-half, -half, half, half};
		float[] xs = new float[4];
		float[] ys = {y, y, y, y};
		float[] zs = new float[4];
		for (int i = 0; i < 4; i++) {
			xs[i] = cx + lx[i] * cs - lz[i] * sn;
			zs[i] = cz + lx[i] * sn + lz[i] * cs;
		}
		quad(c, p, xs, ys, zs, u0, v0, u1, v1, col);
	}

	/** Cuadrado que mira siempre a la camara. */
	private static void board(VertexConsumer c, PoseStack.Pose p, float cx, float cy, float cz, Vector3f right, Vector3f up,
							  float half, float u0, float v0, float u1, float v1, int col) {
		float rx = right.x * half, ry = right.y * half, rz = right.z * half;
		float ux = up.x * half, uy = up.y * half, uz = up.z * half;
		float[] xs = {cx - rx + ux, cx + rx + ux, cx + rx - ux, cx - rx - ux};
		float[] ys = {cy - ry + uy, cy + ry + uy, cy + ry - uy, cy - ry - uy};
		float[] zs = {cz - rz + uz, cz + rz + uz, cz + rz - uz, cz - rz - uz};
		quad(c, p, xs, ys, zs, u0, v0, u1, v1, col);
	}

	/** Plano vertical para el haz de luz. */
	private static void beam(VertexConsumer c, PoseStack.Pose p, float cx, float y0, float y1, float cz, float half, float ang, int col) {
		float dx = (float) Math.cos(ang) * half;
		float dz = (float) Math.sin(ang) * half;
		float[] xs = {cx - dx, cx + dx, cx + dx, cx - dx};
		float[] ys = {y1, y1, y0, y0};
		float[] zs = {cz - dz, cz + dz, cz + dz, cz - dz};
		quad(c, p, xs, ys, zs, 0f, 0f, 1f, 1f, col);
	}
}
