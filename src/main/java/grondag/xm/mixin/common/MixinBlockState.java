/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.xm.mixin.common;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import grondag.xm.api.block.WorldToModelStateFunction;
import grondag.xm.api.block.XmBlockState;
import grondag.xm.api.modelstate.ModelState.Mutable;
import grondag.xm.block.XmBlockStateAccess;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

@Mixin(BlockState.class)
public class MixinBlockState implements XmBlockState, XmBlockStateAccess {
    private WorldToModelStateFunction<?> worldStateFunc = null;

    @Override
    public BlockState blockState() {
        return (BlockState)(Object)this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Mutable> T getModelState(BlockView world, BlockPos pos, boolean refreshFromWorld) {
        final WorldToModelStateFunction<?> func = worldStateFunc;
        return func == null ? null : (T) func.apply((BlockState)(Object)this, world, pos, refreshFromWorld);
    }

    @Override
    public void xm2_worldFunc(WorldToModelStateFunction<?> func) {
        worldStateFunc = func;
    }

    @Override
    public WorldToModelStateFunction<?> xm2_worldFunc() {
        return worldStateFunc;
    }
    
    @Override
    @Nullable
    public XmBlockState xm2_toXmBlockState() {
        return this.worldStateFunc == null ? null : (XmBlockState)this;
    }
}