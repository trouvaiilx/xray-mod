package io.github.trouvaiilx.xray.mixin.sodium;

import io.github.trouvaiilx.xray.XrayState;
import io.github.trouvaiilx.xray.config.XrayConfig;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified against CaffeineMC/sodium, dev branch, commit 27bbd7f (2026-08-07),
 * common/.../render/chunk/compile/pipeline/DefaultFluidRenderer.java
 *
 * Fluids (water/lava) are meshed by an ENTIRELY SEPARATE code path from BlockRenderer --
 * see ChunkBuilderMeshingTask, which calls blockRenderer.renderModel(...) for the block model
 * and, independently, fluidRenderer.render(...) whenever a block has a non-empty FluidState.
 */
@Mixin(DefaultFluidRenderer.class)
public abstract class DefaultFluidRendererMixin {

    @Shadow
    private QuadLightData quadLightData;

    @Shadow
    private float[] brightness;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void xray$skipNonWhitelistedFluids(
            LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos origin,
            TranslucentGeometryCollector collector, ChunkModelBuilder builder, Material material,
            ColorProvider<FluidState> colorProvider, FluidModel model, CallbackInfo ci) {
        if (XrayState.isEnabled()) {
            Block fluidBlock = blockState.getBlock();
            if (fluidBlock == Blocks.AIR) {
                fluidBlock = fluidState.createLegacyBlock().getBlock();
            }
            boolean whitelisted = XrayState.isWhitelisted(fluidBlock);
            if (!whitelisted) {
                ci.cancel();
            } else if (!XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "isFullBlockFluidSideVisible", at = @At("HEAD"), cancellable = true)
    private void xray$alwaysShowFluidSide(BlockGetter view, BlockPos selfPos, Direction facing, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled() && XrayConfig.isWithinXrayDistance(selfPos.getX(), selfPos.getZ())) {
            BlockState neighborState = view.getBlockState(selfPos.relative(facing));

            // Don't render internal faces between the same fluid (e.g. water next to water in oceans)
            if (neighborState.getFluidState().getType().isSame(fluid.getType())) {
                cir.setReturnValue(false);
                return;
            }

            BlockPos neighborPos = selfPos.relative(facing);
            boolean neighborHidden = !XrayState.isWhitelisted(neighborState.getBlock())
                    || !XrayConfig.isWithinXrayDistance(neighborPos.getX(), neighborPos.getZ());

            // If the neighbor block is hidden by X-ray, render the fluid face touching it
            if (neighborHidden) {
                cir.setReturnValue(true);
                return;
            }

            // If the neighbor block IS whitelisted and visible, let Sodium's default occlusion logic decide
        }
    }

    /**
     * isFluidSideExposed is overloaded (a 5-arg (world, ownState, neighborPos, facing, height)
     * convenience wrapper that just forwards into this 4-arg root implementation) -- Mixin needs
     * the full bytecode descriptor here to target the correct overload unambiguously rather than
     * erroring on "multiple candidate methods found."
     *
     * NOTE on X-ray render distance here: unlike the other two fluid/block mixins above and
     * below, neither this method nor isFluidSelfVisible receives a BlockPos -- only
     * BlockStates -- so there's no position to check against XrayConfig#isWithinXrayDistance
     * without a much more invasive change (capturing a local variable from
     * ChunkBuilderMeshingTask's meshing loop across a different mixin target class). Practical
     * impact is small and cosmetic only: a fluid pocket sitting exactly on the X-ray distance
     * boundary may keep an extra exposed face or two just past the configured distance, in a
     * mod whose entire purpose is already "show things through walls." Everything else (which
     * blocks get hidden at all, ore/fluid fullbright, the fluid checks that DO have a pos)
     * still respects the distance setting precisely.
     */
    @Inject(method = "isFluidSideExposed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;F)Z",
            at = @At("HEAD"), cancellable = true)
    private void xray$alwaysExposeFluidSide(BlockState ownBlockState, BlockState neighborBlockState, Direction facing, float height, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled() && height > 0.0F) {
            // Expose the face if the neighbor block is hidden by X-ray (not whitelisted)
            if (!XrayState.isWhitelisted(neighborBlockState.getBlock())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isFluidSelfVisible", at = @At("HEAD"), cancellable = true)
    private void xray$alwaysShowFluidSelf(BlockState selfBlockState, Direction facing, VoxelShape fluidShape, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled()) {
            // Show fluid if the containing block is hidden by X-ray (not whitelisted)
            if (!XrayState.isWhitelisted(selfBlockState.getBlock())) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Force fullbright lighting and disable ambient occlusion shading on fluids (water & lava)
     * while X-ray is enabled, matching the fullbright effect applied to ores.
     */
    @Inject(method = "updateQuad", at = @At("RETURN"))
    private void xray$forceFullbrightForFluids(ModelQuadViewMutable quad, LevelSlice level, BlockPos pos, LightPipeline lighter, Direction dir, ModelQuadFacing facing, float brightness, ColorProvider<FluidState> colorProvider, FluidState fluidState, CallbackInfo ci) {
        if (XrayState.isEnabled() && XrayConfig.isFullbright()
                && XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ())) {
            for (int i = 0; i < 4; i++) {
                this.quadLightData.lm[i] = LightCoordsUtil.FULL_BRIGHT;
                this.brightness[i] = 1.0F;
            }
        }
    }
}
