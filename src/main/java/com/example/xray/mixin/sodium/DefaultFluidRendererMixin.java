package com.example.xray.mixin.sodium;

import com.example.xray.XrayState;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
 * actually broke into it. This mixin forces all three of DefaultFluidRenderer's visibility
 * gates to report "visible" while X-ray is on, so lava/water always show through, regardless
 * of what's (invisibly) surrounding them.
 */
@Mixin(DefaultFluidRenderer.class)
public abstract class DefaultFluidRendererMixin {

    @Inject(method = "isFullBlockFluidSideVisible", at = @At("HEAD"), cancellable = true)
    private void xray$alwaysShowFluidSide(BlockGetter view, BlockPos selfPos, Direction facing, FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled()) {
            cir.setReturnValue(true);
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
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isFluidSelfVisible", at = @At("HEAD"), cancellable = true)
    private void xray$alwaysShowFluidSelf(BlockState selfBlockState, Direction facing, VoxelShape fluidShape, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
