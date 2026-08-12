package com.example.xray.mixin.sodium;

import com.example.xray.XrayState;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified against CaffeineMC/sodium, dev branch, commit 27bbd7f (2026-08-07),
 * common/src/main/java/net/caffeinemc/mods/sodium/client/render/model/AbstractBlockRenderContext.java
 *
 * Without the isFaceCulled fix below, ore looks "patchy": Sodium culls a quad whenever the
 * REAL neighboring block is solid (isFaceCulled -> shouldDrawSide), and that check runs
 * against the actual unmodified world state — it has no idea the neighboring stone is being
 * hidden by our other mixin. So an ore block fully buried in stone would have every face
 * culled and never actually be visible, even though the stone around it is invisible.
 *
 * `state` is the block currently being processed by the render context; BlockRenderer (which
 * extends this class) sets it at the top of renderModel() before emitting any quads, so it's
 * always valid by the time either method below runs during that same call.
 */
@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractBlockRenderContextMixin {

    @Shadow
    protected BlockState state;

    @Shadow
    protected QuadLightData quadLightData;

    @Inject(method = "isFaceCulled", at = @At("HEAD"), cancellable = true)
    private void xray$neverCullWhitelistedBlocks(@Nullable Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (this.state != null && XrayState.isEnabled() && XrayState.isWhitelisted(this.state.getBlock())) {
            cir.setReturnValue(false);
        }
    }

    /**
     * "Fullbright ore" -- first attempt at this only forced the `emissive` parameter true,
     * which turned out to be INSUFFICIENT: shadeQuad's emissive branch only overrides
     * quad.setLight(...) (the lightmap texture coordinate -- simulated torch/sky light).
     * It does NOT touch this.quadLightData.br, a SEPARATE per-vertex ambient-occlusion
     * brightness multiplier that lighter.calculate(...) always fills in regardless of the
     * emissive flag, and which BlockRenderer.bufferQuad(quad, this.quadLightData.br, ...)
     * later multiplies into the final vertex color (see BlockRenderer#processQuad, which
     * calls shadeQuad(...) then immediately bufferQuad(quad, this.quadLightData.br, ...)).
     * An ore block still deep inside real (just invisibly-rendered) stone gets heavy AO
     * shadowing baked into `br`, which was silently crushing the color back down to near-black
     * regardless of the lightmap value -- which is exactly why nothing appeared lit at all.
     *
     * Fix: inject at the TAIL of shadeQuad (after the normal calculation has already run) and,
     * for whitelisted blocks, directly overwrite BOTH the quad's lightmap coordinates AND
     * this.quadLightData.br for all 4 vertices -- replicating what "emissive" was supposed to
     * achieve, but covering the AO multiplier that the emissive branch alone leaves untouched.
     */
    @Inject(method = "shadeQuad", at = @At("RETURN"))
    private void xray$forceFullbrightForWhitelistedBlocks(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, SodiumShadeMode shadeMode, CallbackInfo ci) {
        if (this.state != null && XrayState.isEnabled() && XrayState.isWhitelisted(this.state.getBlock())) {
            for (int i = 0; i < 4; i++) {
                quad.setLight(i, LightCoordsUtil.FULL_BRIGHT);
                this.quadLightData.br[i] = 1.0F;
            }
        }
    }
}


