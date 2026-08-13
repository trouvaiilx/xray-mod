package com.example.xray.mixin.sodium;

import com.example.xray.XrayState;
import com.example.xray.config.XrayConfig;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified against CaffeineMC/sodium, dev branch, commit 27bbd7f (2026-08-07),
 * common/.../render/chunk/compile/tasks/ChunkBuilderMeshingTask.java
 *
 * THIS IS THE ROOT CAUSE OF THE "flashes visible while moving the mouse, disappears while
 * standing still" bug. It has nothing to do with the other mixins' per-quad culling -- it's a
 * separate, section-level occlusion system that the other mixins never touched.
 *
 * While meshing a chunk section, ChunkBuilderMeshingTask ALSO builds a per-section
 * "DirectionalVisGraph" (execute(), around line 150) purely from the real, unmodified
 * BlockState#isSolidRender() of every block in the section:
 *
 *     if (blockState.isSolidRender()) {
 *         occluder.setOpaque(localX, localY, localZ);
 *     }
 *
 * That graph is resolved into a VisibilitySet (see VisibilityEncoding/DirectionalVisGraph)
 * that records which of the section's 6 faces have an "open" path to each other through the
 * section. Every frame, OcclusionCuller does a breadth-first search outward from the camera's
 * own section, and ONLY continues into a neighboring section through a face-pair that the
 * neighbor's own VisibilitySet reports as open (see OcclusionCuller#initWithinWorld ->
 * getConnections -> visitNeighbors). A section whose graph reports no open path is never
 * enqueued and never rendered by that search, full stop -- regardless of what
 * BlockRendererMixin/AbstractBlockRenderContextMixin do to its mesh.
 *
 * Since this graph is built from the REAL BlockState, a section that's mostly hidden stone
 * (still solid as far as the game is concerned, just invisibly rendered by BlockRendererMixin)
 * is still reported as a closed box with no open faces, exactly like it would be without X-ray
 * at all. Two consequences follow:
 *
 *   1. Ore/fluid buried more than one chunk section away from the camera, behind that "closed"
 *      stone, is unreachable by the graph search and literally never gets drawn -- X-ray only
 *      ever worked immediately around the player.
 *
 *   2. For sections in the ring immediately touching the camera's own section, OcclusionCuller
 *      has a SEPARATE fallback (addNearbySections()) that bypasses the graph search entirely
 *      and instead re-tests those sections against the current view frustum every single
 *      frame, independent of occlusion. Since the camera's frustum changes continuously while
 *      looking around, that frustum-only test flips true/false from frame to frame for a
 *      section that the (unfixed) occlusion graph considers "closed" -- which is exactly the
 *      flashing: visible while the mouse is moving (frustum sweeping back onto the section),
 *      gone the instant it settles (graph search takes back over and the section is closed).
 *
 * Fix: make the occlusion graph agree with what's actually drawn. A block that
 * BlockRendererMixin is currently making fully invisible must not register as opaque here
 * either, so the section reports the same "open" connectivity that its rendered (X-rayed)
 * contents actually have. Whitelisted blocks (ore, water, lava) keep their real
 * isSolidRender() value -- they're still individual opaque voxels sitting inside an otherwise
 * open section, same as vanilla.
 */
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskMixin {

    // Declared on the parent ChunkBuilderTask<OUTPUT>
    // (common/.../render/chunk/compile/tasks/ChunkBuilderTask.java:26) as
    // `protected final RenderSection section;` -- shadowing it here (rather than only in a
    // hypothetical ChunkBuilderTaskMixin) works because Mixin resolves @Shadow against the
    // whole target class hierarchy, and this class only ever needs it from within its own
    // injected methods below.
    @Shadow
    @Final
    private net.caffeinemc.mods.sodium.client.render.chunk.RenderSection section;

    // One ChunkBuilderMeshingTask instance = one execute() call on one worker thread (see
    // ChunkBuilderTask's constructor -- a fresh task object is built per meshing job, never
    // reused across jobs), so a plain instance field here is exactly as thread-safe as the
    // local variables execute() itself uses; no atomics/volatile needed.
    @Unique
    private boolean xray$sectionWithinDistance = true;

    @Inject(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At("HEAD")
    )
    private void xray$computeSectionDistance(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        // Render distance is checked at section granularity here (same granularity Sodium
        // itself meshes at), not per-block like the other three mixins -- there's no
        // per-block position available at this call site without capturing a local variable
        // from deep inside execute()'s triple-nested loop (see BlockRendererMixin/
        // AbstractBlockRenderContextMixin for the per-block version, which DO have a pos).
        // Section granularity also matches what a player actually perceives as "X-ray render
        // distance": ore either is or isn't X-rayed within roughly that many chunks, not
        // block-by-block.
        this.xray$sectionWithinDistance = XrayConfig.isChunkWithinXrayDistance(
                this.section.getOriginX() >> 4, this.section.getOriginZ() >> 4);
    }

    // ChunkBuilderTask<T> declares an abstract execute(...) that this class implements with a
    // concrete ChunkBuildOutput return type, which the compiler backs with a synthetic bridge
    // method of the same name -- giving Mixin two same-named candidates unless we pin the exact
    // descriptor here, same reasoning as the overload disambiguation already done in
    // DefaultFluidRendererMixin for isFluidSideExposed.
    @Redirect(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z")
    )
    private boolean xray$dontOccludeHiddenBlocks(BlockState state) {
        if (XrayState.isEnabled() && this.xray$sectionWithinDistance && !XrayState.isWhitelisted(state.getBlock())) {
            // BlockRendererMixin is cancelling this block's mesh entirely (it's invisible),
            // so it must not close off the section's occlusion graph either.
            return false;
        }
        return state.isSolidRender();
    }
}
