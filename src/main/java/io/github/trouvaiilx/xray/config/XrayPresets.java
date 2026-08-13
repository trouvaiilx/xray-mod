package io.github.trouvaiilx.xray.config;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Named whitelist contents for the preset buttons in {@code XrayConfigScreen}. Selecting a
 * preset just replaces the current whitelist with one of the fixed sets below (or, for
 * CUSTOM, leaves whatever's currently selected alone) -- it's a starting point, not a lock;
 * the user can still add/remove individual blocks afterwards (which flips the active preset
 * back to CUSTOM, see XrayConfig#addToWhitelist/removeFromWhitelist).
 */
public final class XrayPresets {
    public static final String DEFAULT = "Default";
    public static final String ORES = "Ores";
    public static final String VALUABLE = "Valuable Blocks";
    public static final String STRUCTURES = "Structures";
    public static final String CUSTOM = "Custom";

    public static final String[] SELECTABLE = {DEFAULT, ORES, VALUABLE, STRUCTURES};

    private XrayPresets() {
    }

    /**
     * CUSTOM has no fixed contents of its own -- it just means "keep whatever's whitelisted
     * right now," so it reads back the live config instead of a static list.
     */
    public static Set<String> blockIds(String preset) {
        return switch (preset) {
            case ORES -> ores();
            case VALUABLE -> valuable();
            case STRUCTURES -> structures();
            case CUSTOM -> new LinkedHashSet<>(XrayConfig.getWhitelistIds());
            default -> defaultPreset();
        };
    }

    private static Set<String> defaultPreset() {
        Set<String> ids = ores();
        // Explicit product requirement: Default ships with water/lava visible, since spotting
        // underground fluid (lava especially) while mining is useful on its own, independent
        // of ore-finding. Still just two ordinary whitelist entries -- removable like anything
        // else.
        ids.add("minecraft:water");
        ids.add("minecraft:lava");
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

    private static Set<String> valuable() {
        Set<String> ids = new LinkedHashSet<>();
        String[] valuables = {
                "diamond_ore", "deepslate_diamond_ore", "ancient_debris",
                "emerald_ore", "deepslate_emerald_ore",
                "gold_ore", "deepslate_gold_ore", "nether_gold_ore",
                "lapis_ore", "deepslate_lapis_ore",
                "copper_ore", "deepslate_copper_ore",
                // Loot containers count as "valuable" the way most X-ray users mean it.
                "chest", "trapped_chest", "ender_chest", "barrel",
        };
        for (String b : valuables) {
            ids.add("minecraft:" + b);
        }
        return ids;
    }

    private static Set<String> structures() {
        Set<String> ids = new LinkedHashSet<>();
        String[] structureBlocks = {
                "spawner", "chest", "trapped_chest", "ender_chest", "barrel",
                "brewing_stand", "crafting_table", "furnace", "blast_furnace", "smoker",
                "bookshelf", "lectern", "enchanting_table", "anvil",
                "cobblestone", "mossy_cobblestone", "stone_bricks", "mossy_stone_bricks",
                "cracked_stone_bricks", "chiseled_stone_bricks", "infested_stone_bricks",
                "end_portal_frame", "nether_bricks", "red_nether_bricks",
        };
        for (String b : structureBlocks) {
            ids.add("minecraft:" + b);
        }
        return ids;
    }
}
