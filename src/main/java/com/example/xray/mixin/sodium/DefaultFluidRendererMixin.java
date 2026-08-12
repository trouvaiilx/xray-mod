package com.example.xray.mixin.sodium;

import com.example.xray.XrayState;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockGetter;
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
 * That means BlockRendererMixin never touched fluid rendering at all, and fluids were already
 * unconditionally exempt from being hidden by X-ray -- so a lava/water SOURCE block was never
 * the problem.
 *
 * The actual problem: DefaultFluidRenderer culls individual fluid FACES based on the real
 * (unmodified) neighboring BlockState's occlusion shape -- exactly like the ore face-culling
 * issue AbstractBlockRenderContextMixin already fixes, just in fluids' own separate culling
 * logic. A lava pocket fully surrounded by stone that X-ray is hiding still has every face
 * culled against that (still solid, just invisible) stone, so you'd see nothing until you
 * actually broke into it.
 */
@Mixin(DefaultFluidRenderer.class)
public abstract class DefaultFluidRendererMixin {

    @Shadow
    private QuadLightData quadLightData;

    @Shadow
    private float[] brightness;

    @Inject(method = "isFullBlockFluidSideVisible", at = @At("HEAD"), cancellable = true)
    private void xray$alwaysShowFluidSide(BlockGetter view, BlockPos selfPos, Direction facing, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled()) {
            BlockState neighborState = view.getBlockState(selfPos.relative(facing));

            // Don't render internal faces between the same fluid (e.g. water next to water in oceans)
            if (neighborState.getFluidState().getType().isSame(fluid.getType())) {
                cir.setReturnValue(false);
                return;
            }

            // If the neighbor block is hidden by X-ray (not whitelisted), render the fluid face touching it
            if (!XrayState.isWhitelisted(neighborState.getBlock())) {
                cir.setReturnValue(true);
                return;
            }

            // If the neighbor block IS whitelisted (e.g., ore), let Sodium's default occlusion logic decide
        }
    }

    /**
     * isFluidSideExposed is overloaded (a 5-arg (world, ownState, neighborPos, facing, height)
     * convenience wrapper that just forwards into this 4-arg root implementation) -- Mixin needs
     * the full bytecode descriptor here to target the correct overload unambiguously rather than
     * erroring on "multiple candidate methods found."
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
        if (XrayState.isEnabled()) {
            for (int i = 0; i < 4; i++) {
                this.quadLightData.lm[i] = LightCoordsUtil.FULL_BRIGHT;
                this.brightness[i] = 1.0F;
            }
        }
    }
}
