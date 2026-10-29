package it.minebuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

/**
 * Conservative, read-only classification for future construction previews.
 * UNKNOWN must never be overwritten automatically.
 */
public final class WorldSafety {
    public enum Classification { EMPTY, NATURAL_TERRAIN, PROTECTED_OR_UNKNOWN }

    private WorldSafety() {}

    public static Classification classify(BlockState state) {
        if (state.isAir()) return Classification.EMPTY;
        if (state.isOf(Blocks.STONE) || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.PODZOL) || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND)
                || state.isOf(Blocks.DEEPSLATE) || state.isOf(Blocks.TUFF)
                || state.isOf(Blocks.ANDESITE) || state.isOf(Blocks.DIORITE)
                || state.isOf(Blocks.GRANITE) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
            return Classification.NATURAL_TERRAIN;
        }
        return Classification.PROTECTED_OR_UNKNOWN;
    }

    /**
     * Classification is not proof of block origin: a player may have placed stone.
     * No destructive operation should use this alone without a preview and backup.
     */
    public static boolean requiresApproval(BlockState state) {
        return classify(state) == Classification.PROTECTED_OR_UNKNOWN;
    }
}
