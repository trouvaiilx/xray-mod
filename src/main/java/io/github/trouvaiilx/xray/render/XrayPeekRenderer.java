package io.github.trouvaiilx.xray.render;

import io.github.trouvaiilx.xray.XrayState;
import io.github.trouvaiilx.xray.config.XrayConfig;
import io.github.trouvaiilx.xray.util.ContainerEntityClassifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * High-performance, allocation-free Peek Mode wireframe renderer.
 *
 * Outlines local block boundaries and container entities (chest minecarts, chest boats, etc.)
 * within a configurable radius of the player camera using Minecraft 26.2's native Gizmos API.
 */
public final class XrayPeekRenderer {

    private static final Direction[] DIRECTIONS = Direction.values();

    // Reusable single-thread mutable positions to avoid GC allocations during spatial scanning
    private static final BlockPos.MutableBlockPos SCAN_POS = new BlockPos.MutableBlockPos();
    private static final BlockPos.MutableBlockPos NEIGHBOR_POS = new BlockPos.MutableBlockPos();

    // Cached GizmoStyle state to avoid constructing new styles every tick when config is unchanged
    private static int lastArgb = 0;
    private static float lastThickness = 0.0F;
    private static GizmoStyle cachedStyle = null;

    private XrayPeekRenderer() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!XrayState.isEnabled() || !XrayConfig.isPeekEnabled()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                return;
            }

            Level level = mc.level;
            BlockPos playerPos = mc.player.blockPosition();

            int radius = XrayConfig.getPeekRadius();
            float opacity = XrayConfig.getPeekOpacity() / 100.0F;
            if (opacity <= 0.001F) {
                return;
            }

            int alphaByte = (int) (opacity * 255.0F) & 0xFF;
            int colorRGB = XrayConfig.getPeekColor();
            int argb = (alphaByte << 24) | (colorRGB & 0xFFFFFF);
            float thickness = XrayConfig.getPeekThickness();

            if (cachedStyle == null || lastArgb != argb || lastThickness != thickness) {
                lastArgb = argb;
                lastThickness = thickness;
                cachedStyle = GizmoStyle.stroke(argb, thickness);
            }

            GizmoStyle style = cachedStyle;

            int px = playerPos.getX();
            int py = playerPos.getY();
            int pz = playerPos.getZ();

            int minX = px - radius;
            int maxX = px + radius;
            int minY = Math.max(level.getMinY(), py - radius);
            int maxY = Math.min(level.getMaxY(), py + radius);
            int minZ = pz - radius;
            int maxZ = pz + radius;

            double playerX = mc.player.getX();
            double playerY = mc.player.getY();
            double playerZ = mc.player.getZ();

            double radiusSqPlusMargin = (radius * radius) + 2.0;

            // 1. Zero-allocation scan of block boundaries
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        double dx = x + 0.5 - playerX;
                        double dy = y + 0.5 - playerY;
                        double dz = z + 0.5 - playerZ;

                        if (dx * dx + dy * dy + dz * dz > radiusSqPlusMargin) {
                            continue;
                        }

                        SCAN_POS.set(x, y, z);
                        BlockState state = level.getBlockState(SCAN_POS);

                        if (state.isAir() || XrayState.isWhitelisted(state.getBlock())) {
                            continue;
                        }

                        boolean isSolid = state.isSolidRender();
                        VoxelShape shape = null;

                        if (!isSolid) {
                            shape = state.getShape(level, SCAN_POS);
                            if (shape.isEmpty()) {
                                continue;
                            }
                        }

                        // Fast check: touches exposed air/fluid/non-whitelisted space
                        boolean touchesExposedSpace = false;
                        for (Direction dir : DIRECTIONS) {
                            NEIGHBOR_POS.setWithOffset(SCAN_POS, dir);
                            BlockState neighborState = level.getBlockState(NEIGHBOR_POS);
                            if (neighborState.isAir() || XrayState.isWhitelisted(neighborState.getBlock())
                                    || !neighborState.isSolidRender()) {
                                touchesExposedSpace = true;
                                break;
                            }
                        }

                        if (touchesExposedSpace) {
                            if (isSolid) {
                                // Full solid block: fast direct cuboid call
                                Gizmos.cuboid(SCAN_POS.immutable(), style);
                            } else {
                                // Non-full block (slabs, stairs, fences): zero-allocation forAllBoxes callback
                                if (shape == null) {
                                    shape = state.getShape(level, SCAN_POS);
                                }
                                if (!shape.isEmpty()) {
                                    final int bx = x;
                                    final int by = y;
                                    final int bz = z;
                                    shape.forAllBoxes((minX1, minY1, minZ1, maxX1, maxY1, maxZ1) -> {
                                        AABB box = new AABB(bx + minX1, by + minY1, bz + minZ1, bx + maxX1, by + maxY1, bz + maxZ1);
                                        Gizmos.cuboid(box, style);
                                    });
                                } else {
                                    Gizmos.cuboid(SCAN_POS.immutable(), style);
                                }
                            }
                        }
                    }
                }
            }

            // 2. Optimized container entity scan (Chest Boats, Minecarts with Chests)
            AABB searchArea = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            List<Entity> entities = level.getEntities((Entity) null, searchArea, ContainerEntityClassifier::isContainerEntity);
            for (Entity entity : entities) {
                if (entity == mc.player) {
                    continue;
                }
                double dx = entity.getX() - playerX;
                double dy = entity.getY() - playerY;
                double dz = entity.getZ() - playerZ;
                if (dx * dx + dy * dy + dz * dz <= radiusSqPlusMargin) {
                    Gizmos.cuboid(entity.getBoundingBox(), style);
                }
            }
        });
    }
}
