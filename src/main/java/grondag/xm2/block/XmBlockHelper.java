package grondag.xm2.block;



import grondag.xm2.state.ModelState;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * Convenience methods for XM Blocks
 */
public class XmBlockHelper {
    /**
     * returns null if not an XM block at the position
     */
    public static ModelState getModelStateIfAvailable(BlockView world, BlockPos pos, boolean refreshFromWorldIfNeeded) {
        return getModelStateIfAvailable(world, world.getBlockState(pos), pos, refreshFromWorldIfNeeded);
    }

    /**
     * returns null if not an XM block at the position
     */
    public static ModelState getModelStateIfAvailable(BlockView world, BlockState state, BlockPos pos, boolean refreshFromWorldIfNeeded) {
        if (state.getBlock() instanceof XmSimpleBlock) {
            return ((XmSimpleBlock)state.getBlock()).getModelStateAssumeStateIsCurrent(state, world, pos, refreshFromWorldIfNeeded);
        } else {
            return null;
        }
    }
    
    /**
     * Returns species at position if it could join with the given block/modelState
     * Returns -1 if no XM block at position or if join not possible.
     */
    public static int getJoinableSpecies(BlockView world, BlockPos pos, BlockState withBlockState, ModelState withModelState) {
        if (withBlockState == null || withModelState == null)
            return -1;

        if (!withModelState.hasSpecies())
            return -1;

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == withBlockState.getBlock()) {
            ModelState mState = getModelStateIfAvailable(world, pos, false);
            if (mState == null)
                return -1;

            if (mState.doShapeAndAppearanceMatch(withModelState))
                return mState.getSpecies();
        }
        return -1;
    }
}