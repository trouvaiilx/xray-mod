package io.github.trouvaiilx.xray.config;

import net.minecraft.world.level.block.Block;

/**
 * Forwarding alias to core.model.BlockCategory for backwards compatibility.
 */
public enum BlockCategory {
    ALL(io.github.trouvaiilx.xray.core.model.BlockCategory.ALL),
    ORES(io.github.trouvaiilx.xray.core.model.BlockCategory.ORES),
    STORAGE(io.github.trouvaiilx.xray.core.model.BlockCategory.STORAGE),
    REDSTONE(io.github.trouvaiilx.xray.core.model.BlockCategory.REDSTONE),
    STRUCTURES(io.github.trouvaiilx.xray.core.model.BlockCategory.STRUCTURES),
    NETHER(io.github.trouvaiilx.xray.core.model.BlockCategory.NETHER),
    END(io.github.trouvaiilx.xray.core.model.BlockCategory.END),
    UNDERGROUND(io.github.trouvaiilx.xray.core.model.BlockCategory.UNDERGROUND),
    NATURAL(io.github.trouvaiilx.xray.core.model.BlockCategory.NATURAL),
    BUILDING(io.github.trouvaiilx.xray.core.model.BlockCategory.BUILDING),
    OTHER(io.github.trouvaiilx.xray.core.model.BlockCategory.OTHER);

    public final String displayName;
    public final io.github.trouvaiilx.xray.core.model.BlockCategory target;

    BlockCategory(io.github.trouvaiilx.xray.core.model.BlockCategory target) {
        this.target = target;
        this.displayName = target.displayName;
    }

    public static BlockCategory classify(Block block) {
        io.github.trouvaiilx.xray.core.model.BlockCategory c = io.github.trouvaiilx.xray.core.model.BlockCategory.classify(block);
        for (BlockCategory cat : values()) {
            if (cat.target == c) {
                return cat;
            }
        }
        return OTHER;
    }
}
