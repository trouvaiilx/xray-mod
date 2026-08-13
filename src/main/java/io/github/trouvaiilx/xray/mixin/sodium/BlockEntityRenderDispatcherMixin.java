package io.github.trouvaiilx.xray.mixin.sodium;

import io.github.trouvaiilx.xray.XrayState;
import io.github.trouvaiilx.xray.config.XrayConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void xray$cullNonWhitelistedBlockEntities(
            E blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, boolean force,
            CallbackInfoReturnable<S> cir) {
        if (XrayState.isEnabled() && blockEntity != null) {
            Block block = blockEntity.getBlockState().getBlock();
            BlockPos pos = blockEntity.getBlockPos();

            boolean whitelisted = XrayState.isWhitelisted(block);
            boolean withinDistance = XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ());

            if (!whitelisted || !withinDistance) {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "tryExtractRenderState", at = @At("RETURN"))
    private <E extends BlockEntity, S extends BlockEntityRenderState> void xray$fullbrightWhitelistedBlockEntities(
            E blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, boolean force,
            CallbackInfoReturnable<S> cir) {
        if (XrayState.isEnabled() && XrayConfig.isFullbright() && cir.getReturnValue() != null && blockEntity != null) {
            Block block = blockEntity.getBlockState().getBlock();
            BlockPos pos = blockEntity.getBlockPos();

            if (XrayState.isWhitelisted(block) && XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ())) {
                cir.getReturnValue().lightCoords = LightCoordsUtil.FULL_BRIGHT;
            }
        }
    }
}
