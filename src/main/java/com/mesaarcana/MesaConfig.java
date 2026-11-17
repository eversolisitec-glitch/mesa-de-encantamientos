package com.mesaarcana;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/mesaarcana.json: se recarga solo al guardar el archivo. */
public class MesaConfig {
	public boolean enabled = true;
	public boolean ring = true;     // anillos de runas flotando alrededor del libro
	public boolean floor = true;    // circulo magico en el suelo
	public boolean glyphs = true;   // runas sueltas que suben
	public boolean beam = true;     // haz de luz
	public double intensity = 1.0;  // brillo general (0.1 a 2)
	public double speed = 1.0;      // velocidad de las animaciones (0.1 a 3)

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static MesaConfig current;
	private static long lastModified = -1L;
	private static long lastCheck = 0L;

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("mesaarcana.json");
	}

	public static MesaConfig get() {
		long now = System.currentTimeMillis();
		if (current == null) {
			current = load();
			lastCheck = now;
		} else if (now - lastCheck > 2000L) {
			lastCheck = now;
			try {
				Path p = path();
				if (Files.exists(p) && Files.getLastModifiedTime(p).toMillis() != lastModified) {
					current = load();
				}
			} catch (Exception ignored) {
			}
		}
		return current;
	}

	private static MesaConfig load() {
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
			cfg.intensity = Math.max(0.1, Math.min(2.0, cfg.intensity));
			cfg.speed = Math.max(0.1, Math.min(3.0, cfg.speed));
			try (Writer w = Files.newBufferedWriter(p)) {
				GSON.toJson(cfg, w);
			}
			lastModified = Files.getLastModifiedTime(p).toMillis();
		} catch (Exception e) {
			System.err.println("[MesaArcana] No se pudo leer la configuracion: " + e);
		}
		return cfg;
	}
}
