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
package grondag.xm.virtual;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

import net.minecraft.block.entity.BlockEntityType;

/**
 * Only purpose is to exclude tile entities that don't need TESR from chunk
 * rendering loop. Code is identical to SuperTileEntityTESR.
 */
@API(status = INTERNAL)
public class VirtualBlockEntityWithRenderer extends VirtualBlockEntity {
	public VirtualBlockEntityWithRenderer(BlockEntityType<?> blockEntityType) {
		super(blockEntityType);
	}
}
