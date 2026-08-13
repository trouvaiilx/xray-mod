package io.github.trouvaiilx.xray.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.trouvaiilx.xray.XrayClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles thread-safe JSON persistence with write-then-atomic-rename safety.
 */
public final class XrayConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicBoolean DIRTY = new AtomicBoolean(false);
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private XrayConfigManager() {
    }

    public static boolean isLoaded() {
        return LOADED.get();
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("xray-mod.json");
    }

    public static XrayConfigData loadConfig() {
        Path path = getConfigPath();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                XrayConfigData data = GSON.fromJson(reader, XrayConfigData.class);
                if (data != null) {
                    LOADED.set(true);
                    return data;
                }
            } catch (IOException | JsonParseException e) {
                XrayClient.LOGGER.warn("Failed to read xray-mod config, falling back to defaults", e);
            }
        }
        LOADED.set(true);
        return new XrayConfigData();
    }

    public static void markDirty() {
        DIRTY.set(true);
    }

    public static void tick() {
        if (DIRTY.compareAndSet(true, false)) {
            saveNow(io.github.trouvaiilx.xray.core.state.XrayState.exportConfigData());
        }
    }

    public static void saveNow(XrayConfigData data) {
        if (!LOADED.get()) {
            return;
        }
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileSystemException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            XrayClient.LOGGER.warn("Failed to save xray-mod config", e);
        }
    }
}
