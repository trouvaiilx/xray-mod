package io.github.trouvaiilx.xray.core.model;

import java.util.LinkedHashSet;
import java.util.Set;

public final class XrayPresets {
    public static final String DEFAULT = "Default";
    public static final String ORES_ONLY = "Ores Only";
    public static final String FLUIDS_ONLY = "Fluids Only";
    public static final String VALUABLES = "Valuables";
    public static final String CUSTOM = "Custom";

    public static final String[] SELECTABLE = {
            DEFAULT,
            ORES_ONLY,
            FLUIDS_ONLY,
            VALUABLES,
    };

    private XrayPresets() {
    }

    public static Set<String> blockIds(String presetName) {
        if (presetName == null) {
            return defaultPreset();
        }
        return switch (presetName) {
            case ORES_ONLY -> ores();
            case FLUIDS_ONLY -> fluids();
            case VALUABLES -> valuables();
            case DEFAULT -> defaultPreset();
            default -> defaultPreset();
        };
    }

    private static Set<String> defaultPreset() {
        Set<String> ids = new LinkedHashSet<>();
        ids.addAll(ores());
        ids.addAll(fluids());
        ids.addAll(storageAndUtility());
        return ids;
    }

    private static Set<String> ores() {
        Set<String> ids = new LinkedHashSet<>();
        String[] ores = {
                "diamond_ore", "deepslate_diamond_ore",
                "emerald_ore", "deepslate_emerald_ore",
                "gold_ore", "deepslate_gold_ore", "nether_gold_ore",
                "iron_ore", "deepslate_iron_ore",
                "coal_ore", "deepslate_coal_ore",
                "redstone_ore", "deepslate_redstone_ore",
                "lapis_ore", "deepslate_lapis_ore",
                "copper_ore", "deepslate_copper_ore",
                "nether_quartz_ore", "ancient_debris",
        };
        for (String ore : ores) {
            ids.add("minecraft:" + ore);
        }
        return ids;
    }

    private static Set<String> fluids() {
        return Set.of("minecraft:water", "minecraft:lava");
    }

    private static Set<String> storageAndUtility() {
        return Set.of(
                "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest",
                "minecraft:barrel", "minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault"
        );
    }

    private static Set<String> valuables() {
        Set<String> ids = new LinkedHashSet<>();
        ids.add("minecraft:diamond_ore");
        ids.add("minecraft:deepslate_diamond_ore");
        ids.add("minecraft:emerald_ore");
        ids.add("minecraft:deepslate_emerald_ore");
        ids.add("minecraft:ancient_debris");
        ids.addAll(storageAndUtility());
        return ids;
    }
}
