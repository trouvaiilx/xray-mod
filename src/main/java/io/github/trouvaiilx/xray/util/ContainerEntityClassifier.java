package io.github.trouvaiilx.xray.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class ContainerEntityClassifier {

    private ContainerEntityClassifier() {
    }

    public static Block getBlockForEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof MinecartHopper) {
            return Blocks.HOPPER;
        }
        if (entity instanceof MinecartSpawner) {
            return Blocks.SPAWNER;
        }
        if (entity instanceof AbstractMinecartContainer || entity instanceof AbstractChestBoat || entity instanceof ContainerEntity) {
            return Blocks.CHEST;
        }
        return null;
    }

    public static boolean isContainerEntity(Entity entity) {
        return getBlockForEntity(entity) != null;
    }
}
