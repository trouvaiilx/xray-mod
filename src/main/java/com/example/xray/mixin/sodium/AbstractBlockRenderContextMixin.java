package com.example.xray.mixin.sodium;

import com.example.xray.XrayState;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified against CaffeineMC/sodium, dev branch, commit 27bbd7f (2026-08-07),
 * common/src/main/java/net/caffeinemc/mods/sodium/client/render/model/AbstractBlockRenderContext.java
 *
 * Without this mixin, BlockRendererMixin alone still leaves ore looking "patchy": Sodium culls
 * a quad whenever the REAL neighboring block is solid (isFaceCulled -> shouldDrawSide), and that
 * check runs against the actual unmodified world state — it has no idea the neighboring stone is
 * being hidden by our other mixin. So an ore block fully buried in stone would have every face
 * culled and never actually be visible, even though the stone around it is invisible.
 *
 * This mixin forces isFaceCulled() to return false (never cull) whenever we're currently
 * rendering a whitelisted block and X-ray is on, so ore renders as a full solid shape from
 * every angle regardless of what's touching it.
 *
 * `state` is the block currently being processed by the render context; BlockRenderer (which
 * extends this class) sets it at the top of renderModel() before emitting any quads, so it's
 * always valid by the time isFaceCulled() runs during that same call.
 */
@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractBlockRenderContextMixin {

    @Shadow
    protected BlockState state;

    @Inject(method = "isFaceCulled", at = @At("HEAD"), cancellable = true)
    private void xray$neverCullWhitelistedBlocks(@Nullable Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (this.state != null && XrayState.isEnabled() && XrayState.isWhitelisted(this.state.getBlock())) {
            cir.setReturnValue(false);
        }
    }
}
