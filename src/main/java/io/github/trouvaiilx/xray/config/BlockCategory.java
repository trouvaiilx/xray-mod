package io.github.trouvaiilx.xray.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Which browse tab a block shows up under in the config screen. This is a display-only
 * grouping over the live block registry (see XrayConfigScreen#loadAllBlocks, which iterates
 * BuiltInRegistries.BLOCK directly) -- it never restricts which blocks CAN be whitelisted,
 * it only decides where they're easiest to find while browsing. A block that doesn't match
 * anything below still shows up fine under ALL and OTHER.
 *
 * Classification is a best-effort, id/namespace-based heuristic, not an exhaustive manual
 * per-block list -- it works reasonably for modded blocks that follow similar naming
 * conventions (e.g. "*_ore", "*chest*") without needing to be updated every time Mojang (or
 * another mod) adds a block.
 */
public enum BlockCategory {
    ALL("All Blocks"),
    ORES("Ores & Minerals"),
    UNDERGROUND("Underground"),
    NETHER("Nether"),
    END("End"),
    REDSTONE("Redstone"),
    STRUCTURES("Structures"),
    STORAGE("Storage"),
    NATURAL("Natural"),
    BUILDING("Building"),
    OTHER("Other");

    public final String displayName;

    BlockCategory(String displayName) {
        this.displayName = displayName;
    }

    /** Checked in this fixed order -- first match wins, so nothing shows up twice. */
    public static BlockCategory classify(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();
        boolean vanilla = id.getNamespace().equals("minecraft");

        if (vanilla && isEndBlock(path)) {
            return END;
        }
        if (vanilla && isNetherBlock(path)) {
            return NETHER;
        }
        if (path.endsWith("_ore") || path.equals("ancient_debris") || path.equals("raw_iron_block")
                || path.equals("raw_copper_block") || path.equals("raw_gold_block")) {
            return ORES;
        }
        if (containsAny(path, "chest", "shulker_box", "barrel")) {
            return STORAGE;
        }
        if (containsAny(path, "redstone", "repeater", "comparator", "piston", "observer",
                "target", "lever", "button", "pressure_plate", "tripwire", "daylight_detector",
                "hopper", "dispenser", "dropper", "rail", "lightning_rod")) {
            return REDSTONE;
        }
        if (path.equals("spawner") || containsAny(path, "brewing_stand", "enchanting_table",
                "lectern", "bookshelf", "furnace", "smoker", "anvil", "cauldron", "stronghold")
                || path.equals("crafting_table")) {
            return STRUCTURES;
        }
        if (containsAny(path, "stone", "deepslate", "gravel", "sand", "clay", "tuff",
                "dripstone", "amethyst", "calcite", "andesite", "diorite", "granite", "basalt",
                "blackstone", "obsidian", "dirt")) {
            return UNDERGROUND;
        }
        if (containsAny(path, "log", "leaves", "planks", "wood", "flower", "grass", "water",
                "lava", "ice", "snow", "sapling", "vine", "coral", "kelp", "seagrass", "mushroom",
                "fungus", "moss", "podzol", "mycelium", "cactus", "bamboo")) {
            return NATURAL;
        }
        if (containsAny(path, "brick", "concrete", "terracotta", "wool", "glass", "slab",
                "stairs", "wall", "fence", "door", "carpet", "banner", "candle")) {
            return BUILDING;
        }
        return OTHER;
    }

    private static boolean isEndBlock(String path) {
        return path.startsWith("end_") || path.contains("end_stone") || path.equals("dragon_egg")
                || path.equals("end_portal_frame") || path.startsWith("chorus_")
                || path.startsWith("purpur_") || path.equals("purpur_block");
    }

    private static boolean isNetherBlock(String path) {
        return path.startsWith("nether_") || path.startsWith("crimson_") || path.startsWith("warped_")
                || path.contains("nether") || path.equals("soul_sand") || path.equals("soul_soil")
                || path.equals("magma_block") || path.equals("glowstone") || path.equals("ancient_debris")
                || path.equals("shroomlight");
    }

    private static boolean containsAny(String path, String... needles) {
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
