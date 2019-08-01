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

package grondag.xm2.api.block;

import java.util.function.Function;

import grondag.xm2.api.connect.world.BlockTest;
import grondag.xm2.api.model.ImmutableModelState;
import grondag.xm2.block.WorldToModelStateFunction;
import grondag.xm2.block.XmBlockRegistryImpl;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public interface XmBlockRegistry {
    static void register(
            Block block, 
            Function<BlockState, ImmutableModelState> defaultStateFunc, 
            WorldToModelStateFunction worldStateFunc,
            BlockTest blockJoinTest) {
        
        XmBlockRegistryImpl.register(block, defaultStateFunc, worldStateFunc, blockJoinTest);
    }
}