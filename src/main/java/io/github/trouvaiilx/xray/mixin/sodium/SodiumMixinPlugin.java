package io.github.trouvaiilx.xray.mixin.sodium;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin reads this config's mixin CLASSES structurally via ASM (not a real JVM classload),
 * so listing Sodium-targeting mixins in xray-sodium.mixins.json is always safe even when
 * Sodium isn't installed. This plugin is what actually stops them from being APPLIED —
 * shouldApplyMixin runs before Mixin ever touches the real Sodium target classes.
 */
public final class SodiumMixinPlugin implements IMixinConfigPlugin {
    private boolean sodiumPresent;

    @Override
    public void onLoad(String mixinPackage) {
        this.sodiumPresent = FabricLoader.getInstance().isModLoaded("sodium");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return sodiumPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null; // use the static list from xray-sodium.mixins.json as-is
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
