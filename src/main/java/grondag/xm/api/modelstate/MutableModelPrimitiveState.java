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
package grondag.xm.api.modelstate;

import grondag.xm.api.connect.model.ClockwiseRotation;
import grondag.xm.terrain.TerrainState;
import net.minecraft.util.math.Direction;

public interface MutableModelPrimitiveState extends ModelPrimitiveState {
    MutableModelState setAxis(Direction.Axis axis);

    MutableModelState setAxisInverted(boolean isInverted);

    /**
     * For machines and other blocks with a privileged horizontal face, North is
     * considered the zero rotation.
     */
    MutableModelState setAxisRotation(ClockwiseRotation rotation);

    MutableModelState setTerrainState(TerrainState flowState);

    MutableModelState setTerrainStateKey(long terrainStateKey);

    MutableModelState primitiveBits(int bits);
}
