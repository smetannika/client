package dev.serverhelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки. Код бинда: -1 = не назначен, 0..999 = клавиша GLFW, 1000+N = кнопка мыши N.
 */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("serverhelper.json");

    public int trapKey = -1;
    public int plastKey = -1;
    public int killerKey = -1;
    public int medicKey = -1;

    /** Кидать зелье под ноги (разворот взгляда вниз на момент броска). */
    public boolean killerDown = false;
    public boolean medicDown = true;

    public static Config INSTANCE = new Config();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
                Config c = GSON.fromJson(json, Config.class);
                if (c != null) {
                    INSTANCE = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[ServerHelper] не удалось прочитать конфиг: " + e);
        }
    }

    public static void save() {
        try {
            Files.write(FILE, GSON.toJson(INSTANCE).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[ServerHelper] не удалось сохранить конфиг: " + e);
        }
    }
}
