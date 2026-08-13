package com.example.xray.mixin.sodium;

import com.example.xray.XrayState;
import com.example.xray.config.XrayConfig;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified against CaffeineMC/sodium, dev branch, commit 27bbd7f (2026-08-07),
 * common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer.java
 *
 * BlockRenderer#renderModel(BlockStateModel, BlockState, BlockPos, BlockPos) is called once per
 * non-air block position during chunk meshing (see ChunkBuilderMeshingTask#execute, line ~129).
 * Cancelling it here means the block's quads are never added to the chunk mesh at all — it
 * becomes fully invisible. It does NOT touch collision (that's vanilla world state, untouched)
 * and it does NOT touch the chunk-section occlusion graph (that's set separately from
 * BlockState#isSolidRender() a few lines later in the same loop, so far-away empty-looking
 * chunks still cull correctly).
 *
 * IMPORTANT: this method signature is the thing most likely to change across Sodium releases.
 * If this mixin fails to apply (check your log for a Mixin ERROR mentioning BlockRenderer),
 * re-check this file against whatever Sodium version you've pinned in gradle.properties.
 */
@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void xray$skipNonWhitelistedBlocks(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if (XrayState.isEnabled() && !XrayState.isWhitelisted(state.getBlock())
                && XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ())) {
            ci.cancel();
        }
    }
}
