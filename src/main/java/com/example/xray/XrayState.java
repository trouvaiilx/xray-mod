package com.example.xray;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds the runtime X-ray toggle and the set of blocks that should remain visible.
 * Read from the render thread (mixins) and written from the command thread — both
 * of these only ever happen on the client, and the flag is a plain volatile/atomic,
 * so no extra locking is needed for a simple on/off switch.
 */
public final class XrayState {
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    // Default ore whitelist. Edit freely, or load this from a config file later.
    //
    // isWhitelisted() below is called from BlockRenderer/AbstractBlockRenderContext/
    // DefaultFluidRenderer mixins, i.e. potentially several times per solid block in every
    // chunk section that gets (re)meshed on worker threads. A plain java.util.HashSet<Block>
    // works (Block has no custom equals/hashCode, so it's already identity-based), but every
    // lookup still walks a Node-based bucket. fastutil's ReferenceOpenHashSet does the same
    // identity comparison over a flat open-addressed array with no per-entry object, which is
    // both faster and more cache-friendly in this hot path -- it's the same reasoning Sodium's
    // own Block-keyed hot-path maps use (e.g. ColorProviderRegistry's
    // Reference2ReferenceOpenHashMap<Block, ...>). fastutil is already a transitive dependency
    // via Minecraft itself, so this doesn't add anything to build.gradle.
    private static final Set<Block> WHITELIST = new ReferenceOpenHashSet<>();

    static {
        addOre("minecraft:diamond_ore");
        addOre("minecraft:deepslate_diamond_ore");
        addOre("minecraft:emerald_ore");
        addOre("minecraft:deepslate_emerald_ore");
        addOre("minecraft:gold_ore");
        addOre("minecraft:deepslate_gold_ore");
        addOre("minecraft:iron_ore");
        addOre("minecraft:deepslate_iron_ore");
        addOre("minecraft:redstone_ore");
        addOre("minecraft:deepslate_redstone_ore");
        addOre("minecraft:lapis_ore");
        addOre("minecraft:deepslate_lapis_ore");
        addOre("minecraft:copper_ore");
        addOre("minecraft:deepslate_copper_ore");
        addOre("minecraft:ancient_debris");
        addOre("minecraft:nether_gold_ore");
        addOre("minecraft:nether_quartz_ore");

        // Safety net for the fluid BLOCK's own model, if it has one (belt-and-suspenders --
        // the actual water/lava visibility fix is DefaultFluidRendererMixin, which forces
        // fluid faces visible unconditionally while X-ray is on; that mixin doesn't check
        // this whitelist at all, since fluids are meshed via a separate code path from
        // regular blocks and were never affected by the block-hiding mixin above in the
        // first place). minecraft:water and minecraft:lava cover BOTH source and flowing
        // states -- since ~1.13 they're a single Block per fluid (a LEVEL blockstate
        // property distinguishes them), not separate registry entries.
        addOre("minecraft:water");
        addOre("minecraft:lava");
    }

    private XrayState() {
    }

    private static void addOre(String id) {
        // NOTE: Mojang renamed ResourceLocation -> Identifier back in 1.21.11 -- this carries
        // through to 26.x -- but it stayed in the SAME package, net.minecraft.resources.
        // (An earlier attempt at this fix incorrectly moved the import to net.minecraft.util;
        // confirmed against real current mod source that it's still net.minecraft.resources.)
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (block != null) {
            WHITELIST.add(block);
        }
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
    }

    public static boolean toggle() {
        // NOTE: AtomicBoolean has no accumulateAndGet/updateAndGet (those only exist on
        // AtomicInteger/AtomicLong/AtomicReference) -- that was a real bug, not a version
        // issue.
        //
        // A CAS retry loop here is unnecessary work, not just unnecessary caution: per this
        // class's own doc comment, ENABLED is only ever WRITTEN from the client command
        // thread, and Brigadier executes client commands one at a time on that single thread
        // (see XrayCommand) -- so there is never a second writer for the CAS to lose a race
        // against. AtomicBoolean is still the right type (its get()/set() are volatile reads/
        // writes, which is what actually matters: making the new value visible to the render
        // threads that call isEnabled()), just without the pointless retry loop.
        boolean newValue = !ENABLED.get();
        ENABLED.set(newValue);
        return newValue;
    }

    /**
     * True for blocks that should keep rendering (and render on every face) while X-ray is on.
     */
    public static boolean isWhitelisted(Block block) {
        return WHITELIST.contains(block);
    }
}
