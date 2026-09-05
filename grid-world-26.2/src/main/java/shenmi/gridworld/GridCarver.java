package shenmi.gridworld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class GridCarver {
    private GridCarver() {
    }

    public static ChunkAccess carveAfterNoise(ChunkAccess chunk) {
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = minX + localX;
            boolean xPlane = isGridCoordinate(worldX);

            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = minZ + localZ;
                boolean zPlane = isGridCoordinate(worldZ);

                for (int y = minY; y < maxY; y++) {
                    boolean yPlane = isGridCoordinate(y);
                    int matchedAxes = (xPlane ? 1 : 0) + (yPlane ? 1 : 0) + (zPlane ? 1 : 0);
                    if (matchedAxes >= 2) {
                        continue;
                    }

                    pos.set(worldX, y, worldZ);
                    BlockState current = chunk.getBlockState(pos);
                    if (current.isAir()) {
                        continue;
                    }

                    chunk.setBlockState(pos, air, 0);
                }
            }
        }

        return chunk;
    }

    private static boolean isGridCoordinate(int coordinate) {
        return Math.floorMod(coordinate, GridConfig.spacing()) < GridConfig.thickness();
    }
}
