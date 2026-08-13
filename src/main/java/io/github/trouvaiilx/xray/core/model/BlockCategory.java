package io.github.trouvaiilx.xray.core.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

public enum BlockCategory {
    ALL("All Blocks"),
    ORES("Ores & Minerals"),
    STORAGE("Storage & Chests"),
    REDSTONE("Redstone & Utility"),
    STRUCTURES("Structures & Spawners"),
    NETHER("Nether Blocks"),
    END("End Blocks"),
    UNDERGROUND("Underground & Stone"),
    NATURAL("Plants & Foliage"),
    BUILDING("Building & Wood"),
    OTHER("Other");

    public final String displayName;

    BlockCategory(String displayName) {
        this.displayName = displayName;
    }

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
        if (containsAny(path, "spawner", "sculk_shrieker", "sculk_sensor", "trial_spawner", "vault")) {
            return STRUCTURES;
        }
        if (containsAny(path, "stone", "deepslate", "granite", "diorite", "andesite", "tuff",
                "calcite", "dripstone", "dirt", "grass_block", "gravel", "sand", "sandstone",
                "clay", "obsidian", "crying_obsidian", "magma_block", "ice", "snow")) {
            return UNDERGROUND;
        }
        if (containsAny(path, "leaves", "sapling", "flower", "tulip", "rose", "orchid",
                "allium", "bluet", "daisy", "dandelion", "poppy", "sunflower", "lilac",
                "rose_bush", "peony", "vine", "grass", "fern", "bamboo", "cactus",
                "sugar_cane", "kelp", "seagrass", "coral", "fungus", "roots", "spore_blossom")) {
            return NATURAL;
        }
        if (containsAny(path, "log", "wood", "planks", "stairs", "slab", "fence", "gate",
                "door", "trapdoor", "button", "brick", "terracotta", "concrete", "wool",
                "glass", "stained_glass")) {
            return BUILDING;
        }
        return OTHER;
    }

    private static boolean isNetherBlock(String path) {
        return containsAny(path, "nether", "blackstone", "basalt", "soul_sand", "soul_soil",
                "crimson", "warped", "glowstone", "quartz", "shroomlight");
    }

    private static boolean isEndBlock(String path) {
        return containsAny(path, "end_stone", "purpur", "chorus");
    }

    private static boolean containsAny(String input, String... targets) {
        String lower = input.toLowerCase(Locale.ROOT);
        for (String target : targets) {
            if (lower.contains(target)) {
                return true;
            }
        }
        return false;
    }
}
