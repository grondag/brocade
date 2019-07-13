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

package grondag.xm2.state;

import static grondag.xm2.state.ModelStateData.STATE_FLAG_DISABLE_BLOCK_ONLY;
import static grondag.xm2.state.ModelStateData.STATE_FLAG_HAS_SOLID_RENDER;
import static grondag.xm2.state.ModelStateData.STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY;
import static grondag.xm2.state.ModelStateData.STATE_FLAG_HAS_TRANSLUCENT_RENDER;
import static grondag.xm2.state.ModelStateData.STATE_FLAG_IS_POPULATED;
import static grondag.xm2.state.ModelStateData.STATE_FLAG_NEEDS_SPECIES;

import grondag.xm2.mesh.MeshFactory;
import grondag.xm2.mesh.ModelShape;
import grondag.xm2.painting.PaintLayer;
import grondag.xm2.texture.api.TextureSet;
import grondag.xm2.texture.impl.TextureSetRegistryImpl;

/**
 * Populates state flags for a given model state.
 * 
 * Results are returns as STATE_FLAG_XXXX values from ModelState for easy
 * persistence and usage within that class.
 */
public class ModelStateFlagHelper {
    public static final int getFlags(ModelState state) {
        final ModelShape<?> shape = state.getShape();
        final MeshFactory mesh = shape.meshFactory();

        int flags = STATE_FLAG_IS_POPULATED | mesh.getStateFlags(state);

        if (shape.metaUsage() == MetaUsage.SPECIES)
            flags |= STATE_FLAG_NEEDS_SPECIES;

        TextureSet texBase = state.getTexture(PaintLayer.BASE);
        flags |= texBase.stateFlags();

        flags |= state.getTexture(PaintLayer.CUT).stateFlags();

        if (state.isTranslucent(PaintLayer.BASE))
            flags |= (STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY | STATE_FLAG_HAS_TRANSLUCENT_RENDER);
        else
            flags |= STATE_FLAG_HAS_SOLID_RENDER;

        if (state.isTranslucent(PaintLayer.CUT))
            flags |= (STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY | STATE_FLAG_HAS_TRANSLUCENT_RENDER);
        else
            flags |= STATE_FLAG_HAS_SOLID_RENDER;

        if (mesh.hasLampSurface(state)) {
            TextureSet texLamp = state.getTexture(PaintLayer.LAMP);
            if (texLamp.id() != TextureSetRegistryImpl.NONE_ID)
                flags |= texLamp.stateFlags();

            if (state.isTranslucent(PaintLayer.LAMP))
                flags |= (STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY | STATE_FLAG_HAS_TRANSLUCENT_RENDER);
            else
                flags |= STATE_FLAG_HAS_SOLID_RENDER;
        }

        TextureSet texOverlay = state.getTexture(PaintLayer.MIDDLE);
        if (texOverlay.id() != TextureSetRegistryImpl.NONE_ID)
            flags |= (texOverlay.stateFlags() | STATE_FLAG_HAS_TRANSLUCENT_RENDER);

        texOverlay = state.getTexture(PaintLayer.OUTER);
        if (texOverlay.id() != TextureSetRegistryImpl.NONE_ID)
            flags |= (texOverlay.stateFlags() | STATE_FLAG_HAS_TRANSLUCENT_RENDER);

        // turn off this.stateFlags that don't apply to non-block formats if we aren't
        // one
        if (mesh.stateFormat != StateFormat.BLOCK)
            flags &= STATE_FLAG_DISABLE_BLOCK_ONLY;

        return flags;
    }
}
