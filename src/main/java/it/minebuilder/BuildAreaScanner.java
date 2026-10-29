package it.minebuilder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Read-only bounded scan for a future construction preview.
 * Never changes blocks and treats all unknown blocks as protected.
 */
public final class BuildAreaScanner {
    public record ScanResult(long empty, long naturalTerrain, long protectedOrUnknown) {
        public boolean requiresApproval() { return protectedOrUnknown > 0; }
    }

    private BuildAreaScanner() {}

    public static ScanResult scan(World world, BlockPos first, BlockPos second) {
        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minY = Math.max(world.getBottomY(), Math.min(first.getY(), second.getY()));
        int maxY = Math.min(world.getTopY() - 1, Math.max(first.getY(), second.getY()));
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        if (minY > maxY) throw new IllegalArgumentException("Area outside world height");
        long volume = ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1);
        if (volume > 32768L) throw new IllegalArgumentException("Scan too large; split into sections");
        long empty = 0, natural = 0, protectedBlocks = 0;
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    switch (WorldSafety.classify(world.getBlockState(cursor))) {
                        case EMPTY -> empty++;
                        case NATURAL_TERRAIN -> natural++;
                        case PROTECTED_OR_UNKNOWN -> protectedBlocks++;
                    }
                }
            }
        }
        return new ScanResult(empty, natural, protectedBlocks);
    }
}
