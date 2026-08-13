package io.github.trouvaiilx.xray.mixin.sodium;

import io.github.trouvaiilx.xray.XrayState;
import io.github.trouvaiilx.xray.config.XrayConfig;
import io.github.trouvaiilx.xray.util.ContainerEntityClassifier;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void xray$cullNonWhitelistedChestEntities(
            E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (XrayState.isEnabled() && entity != null) {
            Block blockForEntity = ContainerEntityClassifier.getBlockForEntity(entity);
            if (blockForEntity != null) {
                BlockPos pos = entity.blockPosition();
                boolean whitelisted = XrayState.isWhitelisted(blockForEntity);
                boolean withinDistance = XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ());

                if (!whitelisted || !withinDistance) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void xray$fullbrightWhitelistedChestEntities(
            E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        if (XrayState.isEnabled() && XrayConfig.isFullbright() && cir.getReturnValue() != null && entity != null) {
            Block blockForEntity = ContainerEntityClassifier.getBlockForEntity(entity);
            if (blockForEntity != null && XrayState.isWhitelisted(blockForEntity)) {
                BlockPos pos = entity.blockPosition();
                if (XrayConfig.isWithinXrayDistance(pos.getX(), pos.getZ())) {
                    cir.getReturnValue().lightCoords = LightCoordsUtil.FULL_BRIGHT;
                }
            }
        }
    }
}
