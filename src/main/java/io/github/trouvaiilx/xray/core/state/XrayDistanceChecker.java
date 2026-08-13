package io.github.trouvaiilx.xray.core.state;

import net.minecraft.client.Minecraft;

/**
 * Handles Chebyshev spatial range calculations for X-ray chunk rendering.
 */
public final class XrayDistanceChecker {

    private static volatile int playerChunkX = 0;
    private static volatile int playerChunkZ = 0;
    private static volatile boolean playerPosValid = false;

    private XrayDistanceChecker() {
    }

    public static void updatePlayerChunkPos(int cx, int cz) {
        playerChunkX = cx;
        playerChunkZ = cz;
        playerPosValid = true;
    }

    public static boolean isWithinXrayDistance(int blockX, int blockZ) {
        return isChunkWithinXrayDistance(blockX >> 4, blockZ >> 4);
    }

    public static boolean isChunkWithinXrayDistance(int chunkX, int chunkZ) {
        if (!playerPosValid) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return true;
            }
            playerChunkX = player.getBlockX() >> 4;
            playerChunkZ = player.getBlockZ() >> 4;
            playerPosValid = true;
        }
        int px = playerChunkX;
        int pz = playerChunkZ;
        int distance = XrayState.getRenderDistance();
        return Math.max(Math.abs(chunkX - px), Math.abs(chunkZ - pz)) <= distance;
    }
}
