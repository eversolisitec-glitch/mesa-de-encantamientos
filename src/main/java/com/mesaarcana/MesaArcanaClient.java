package com.mesaarcana;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Mesa Arcana (solo cliente).
 * Todo se hace con particulas de vanilla: las letras "ENCHANT" son las runas
 * del alfabeto galactico, y las particulas de polvo dan el color.
 */
public class MesaArcanaClient implements ClientModInitializer {
	private static final double TAU = Math.PI * 2.0;
	private static final Random RNG = new Random();

	// Dibujos de 5x5 para las runas del suelo
	private static final String[][] GLYPHS = {
		{"#####", "....#", "###.#", "#...#", "#.###"},
		{"#.#.#", "#####", "..#..", ".###.", "#...#"},
		{"###..", "..#.#", "#.#.#", "#.#..", "#.###"},
		{".###.", "#...#", "#.#.#", "#...#", ".###."},
	};

	private MesaConfig config = new MesaConfig();
	private final Map<BlockPos, Integer> tables = new HashMap<>();
	private ClientLevel lastLevel;
	private long tick;
	private ParticleOptions cyan;
	private ParticleOptions violet;

	@Override
	public void onInitializeClient() {
		applyConfig(MesaConfig.load());
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void applyConfig(MesaConfig c) {
		config = c;
		cyan = new DustParticleOptions(c.cyanInt(), 0.55f);
		violet = new DustParticleOptions(c.violetInt(), 0.55f);
	}

	private void onTick(Minecraft mc) {
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			tables.clear();
			lastLevel = null;
			return;
		}
		if (level != lastLevel) {
			tables.clear();
			lastLevel = level;
		}
		if (mc.isPaused()) {
			return;
		}
		tick++;

		if (tick % 40 == 0 && MesaConfig.fileChanged()) {
			applyConfig(MesaConfig.load());
		}
		if (!config.enabled) {
			return;
		}
		if (tick % 20 == 1) {
			scan(level, player.blockPosition());
		}

		double maxSq = config.maxDistance * config.maxDistance;
		Iterator<Map.Entry<BlockPos, Integer>> it = tables.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BlockPos, Integer> e = it.next();
			BlockPos p = e.getKey();
			if (!level.getBlockState(p).is(Blocks.ENCHANTING_TABLE)) {
				it.remove();
				continue;
			}
			if (player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > maxSq) {
				continue;
			}
			effects(level, p, e.getValue());
		}
	}

	/** Busca mesas de encantamiento cerca del jugador (1 vez por segundo). */
	private void scan(ClientLevel level, BlockPos center) {
		Map<BlockPos, Integer> found = new HashMap<>();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		int r = (int) Math.min(config.maxDistance, 24);
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				for (int y = -10; y <= 10; y++) {
					m.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					if (level.getBlockState(m).is(Blocks.ENCHANTING_TABLE)) {
						BlockPos table = m.immutable();
						found.put(table, shelfPower(level, table));
					}
				}
			}
		}
		tables.clear();
		tables.putAll(found);
	}

	/** Parecido al poder de vanilla: estanterias a 2 bloques, 0 a 15. */
	private int shelfPower(ClientLevel level, BlockPos p) {
		int n = 0;
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
					continue;
				}
				for (int dy = 0; dy <= 1; dy++) {
					m.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
					if (level.getBlockState(m).is(Blocks.BOOKSHELF)) {
						n++;
					}
				}
			}
		}
		return Math.min(15, n / 2);
	}

	private static boolean roll(double chance) {
		return chance >= 1.0 || RNG.nextDouble() < chance;
	}

	private static void add(ClientLevel level, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
		level.addParticle(o, x, y, z, vx, vy, vz);
	}

	private void effects(ClientLevel level, BlockPos p, int power) {
		double cx = p.getX() + 0.5;
		double cz = p.getZ() + 0.5;
		double by = p.getY();
		double d = config.density;
		double pw = power / 15.0;

		// 1) Anillos de runas que giran alrededor del libro
		if (config.ring) {
			double ry = by + config.ringHeight;
			double radius = 1.3 + 0.06 * Math.sin(tick * 0.06);
			double speed = 0.07 + 0.07 * pw;
			int heads = 2 + power / 4;
			for (int i = 0; i < heads; i++) {
				double a = tick * speed + i * TAU / heads;
				double x = cx + Math.cos(a) * radius;
				double z = cz + Math.sin(a) * radius;
				if (roll(d)) {
					add(level, cyan, x, ry, z, 0, 0, 0);
				}
				if ((tick + i) % 3 == 0 && roll(d)) {
					// la letra nace "detras" y avanza hacia su sitio: parece que gira
					add(level, ParticleTypes.ENCHANT, x, ry + 0.05, z, Math.sin(a) * 0.45, 0, -Math.cos(a) * 0.45);
				}
			}
			// anillo interior, sentido contrario
			double r2 = 0.8;
			double ry2 = ry - 0.3;
			int heads2 = 2 + power / 6;
			for (int i = 0; i < heads2; i++) {
				double a = -tick * speed * 1.4 + i * TAU / heads2;
				double x = cx + Math.cos(a) * r2;
				double z = cz + Math.sin(a) * r2;
				if (roll(d)) {
					add(level, violet, x, ry2, z, 0, 0, 0);
				}
				if ((tick + i) % 5 == 0 && roll(d)) {
					add(level, ParticleTypes.ENCHANT, x, ry2 + 0.05, z, -Math.sin(a) * 0.35, 0, Math.cos(a) * 0.35);
				}
			}
		}

		// 2) Runas sueltas que suben
		if (config.rising && roll(0.25 * d * (0.6 + pw))) {
			double a = RNG.nextDouble() * TAU;
			double rad = 0.6 + RNG.nextDouble() * 1.2;
			double x = cx + Math.cos(a) * rad;
			double z = cz + Math.sin(a) * rad;
			double y = by + 1.3 + RNG.nextDouble() * 1.4;
			add(level, ParticleTypes.ENCHANT, x, y, z, 0, -1.4, 0);
		}

		// 3) Haz de luz vertical
		if (config.beam && roll(0.5 * d * (0.5 + pw))) {
			double a = RNG.nextDouble() * TAU;
			double rad = RNG.nextDouble() * 0.25;
			add(level, ParticleTypes.END_ROD, cx + Math.cos(a) * rad, by + 1.2, cz + Math.sin(a) * rad, 0, 0.12 + RNG.nextDouble() * 0.06, 0);
		}

		// 4) Runas brillantes en el suelo (las 8 casillas de alrededor) + anillo en el suelo
		if (config.floor) {
			double fy = by + 0.04;
			double pulse = 0.55 + 0.45 * Math.sin(tick * 0.09);
			int idx = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					idx++;
					if (!level.getBlockState(p.offset(dx, 0, dz)).isAir()) {
						continue;
					}
					String[] g = GLYPHS[idx % GLYPHS.length];
					int rot = idx % 4;
					ParticleOptions col = (dx == 0 || dz == 0) ? violet : cyan;
					for (int gy = 0; gy < 5; gy++) {
						for (int gx = 0; gx < 5; gx++) {
							if (cell(g, rot, gx, gy) && roll(0.14 * d * pulse * (0.7 + 0.6 * pw))) {
								add(level, col, cx + dx + (gx - 2) * 0.11, fy, cz + dz + (gy - 2) * 0.11, 0, 0, 0);
							}
						}
					}
				}
			}
			for (int i = 0; i < 3; i++) {
				double a = -tick * 0.05 + i * TAU / 3.0;
				if (roll(d)) {
					add(level, violet, cx + Math.cos(a) * 1.38, fy, cz + Math.sin(a) * 1.38, 0, 0, 0);
				}
			}
		}
	}

	private static boolean cell(String[] g, int rot, int x, int y) {
		for (int i = 0; i < rot; i++) {
			int nx = 4 - y;
			int ny = x;
			x = nx;
			y = ny;
		}
		return g[y].charAt(x) == '#';
	}
}
