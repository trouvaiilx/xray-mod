package io.github.trouvaiilx.xray.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.trouvaiilx.xray.XrayClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Server-side configuration for X-Ray Mod.
 */
public final class XrayServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("xray-mod-server.json");

    public static class Data {
        public boolean allowXray = true;
    }

    private static Data data = new Data();

    private XrayServerConfig() {
    }

    public static synchronized void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) {
                    data = loaded;
                }
            } catch (Exception e) {
                XrayClient.LOGGER.error("[X-Ray Server] Failed to load server config", e);
            }
        } else {
            save();
        }
    }

    public static synchronized void save() {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(data, writer);
            }
            Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            XrayClient.LOGGER.error("[X-Ray Server] Failed to save server config", e);
        }
    }

    public static boolean isXrayAllowed() {
        return data.allowXray;
    }

    public static void setXrayAllowed(boolean allowed) {
        data.allowXray = allowed;
        save();
    }
}
