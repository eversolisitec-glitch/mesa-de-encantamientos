package com.mesaarcana;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuracion en config/mesaarcana.json (se recarga sola al guardar el archivo). */
public class MesaConfig {
	public boolean enabled = true;
	public boolean ring = true;      // anillos de runas girando alrededor del libro
	public boolean rising = true;    // runas sueltas que suben
	public boolean beam = true;      // haz de luz vertical
	public boolean floor = true;     // runas brillantes en el suelo
	public double density = 1.0;     // 0.1 a 4 (mas = mas particulas)
	public double ringHeight = 1.45; // altura del anillo sobre la base de la mesa
	public double maxDistance = 24.0;
	public String colorCyan = "3CD7FF";
	public String colorViolet = "9660FF";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static long lastModified = -1L;

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("mesaarcana.json");
	}

	public static MesaConfig load() {
		Path p = path();
		MesaConfig cfg = new MesaConfig();
		try {
			if (Files.exists(p)) {
				try (Reader r = Files.newBufferedReader(p)) {
					MesaConfig read = GSON.fromJson(r, MesaConfig.class);
					if (read != null) {
						cfg = read;
					}
				}
			}
			cfg.sanitize();
			try (Writer w = Files.newBufferedWriter(p)) {
				GSON.toJson(cfg, w);
			}
			lastModified = Files.getLastModifiedTime(p).toMillis();
		} catch (Exception e) {
			System.err.println("[MesaArcana] No se pudo leer la configuracion: " + e);
			cfg.sanitize();
		}
		return cfg;
	}

	public static boolean fileChanged() {
		try {
			Path p = path();
			return Files.exists(p) && Files.getLastModifiedTime(p).toMillis() != lastModified;
		} catch (Exception e) {
			return false;
		}
	}

	private void sanitize() {
		density = Math.max(0.1, Math.min(4.0, density));
		ringHeight = Math.max(0.5, Math.min(3.0, ringHeight));
		maxDistance = Math.max(4.0, Math.min(64.0, maxDistance));
		if (colorCyan == null) colorCyan = "3CD7FF";
		if (colorViolet == null) colorViolet = "9660FF";
	}

	public int cyanInt() {
		return parse(colorCyan, 0x3CD7FF);
	}

	public int violetInt() {
		return parse(colorViolet, 0x9660FF);
	}

	private static int parse(String s, int fallback) {
		try {
			return Integer.parseInt(s.replace("#", "").trim(), 16) & 0xFFFFFF;
		} catch (Exception e) {
			return fallback;
		}
	}
}
