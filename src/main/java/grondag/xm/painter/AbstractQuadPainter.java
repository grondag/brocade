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
package grondag.xm.painter;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

import grondag.fermion.spatial.Rotation;
import grondag.fermion.varia.Useful;
import grondag.xm.api.mesh.MutableMesh;
import grondag.xm.api.mesh.polygon.MutablePolygon;
import grondag.xm.api.modelstate.PrimitiveModelState;
import grondag.xm.api.paint.XmPaint;
import grondag.xm.api.primitive.surface.XmSurface;
import grondag.xm.api.texture.TextureRotation;
import grondag.xm.api.texture.TextureScale;
import grondag.xm.api.texture.TextureSet;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

@API(status = INTERNAL)
@SuppressWarnings("rawtypes")
public abstract class AbstractQuadPainter {
    @FunctionalInterface
    public static interface PaintMethod {
        /**
         * Assigns specific texture and texture rotation based on model state and
         * information in the polygon and surface. Also handles texture UV mapping.
         * <p>
         * 
         * Implementations can and should assume locked UV coordinates are assigned
         * before this is called if UV locking is enabled for the quad
         * <p>
         * 
         * Implementation should claim and use first render layer with a null texture
         * name.<br>
         * (Claim by assigning a non-null texture name.)
         * 
         * Any polys in the input stream that are split should be deleted and new polys
         * appended to the stream.
         * <p>
         * 
         * Implementation may assume stream is non-empty and stream editor is at origin.
         * <p>
         * 
         */
        void paintQuads(MutableMesh stream, PrimitiveModelState modelState, XmSurface surface, XmPaint paint, int textureDepth);
    }

    /**
     * Call from paint quad in sub classes to return results. Handles item scaling,
     * then adds to the output list.
     */
    protected static void commonPostPaint(MutablePolygon editor, PrimitiveModelState modelState, XmSurface surface, XmPaint paint, int textureIndex) {
        editor.blendMode(textureIndex, paint.blendMode(textureIndex));
        editor.emissive(textureIndex, paint.emissive(textureIndex));
        editor.disableAo(textureIndex, paint.disableAo(textureIndex));
        editor.disableDiffuse(textureIndex, paint.disableDiffuse(textureIndex));
        paint.vertexProcessor(textureIndex).process(editor, modelState, surface, paint, textureIndex);
    }

    /**
     * Transform input vector so that x & y correspond with u / v on the given face,
     * with u,v origin at upper left and z is depth, where positive values represent
     * distance into the face (away from viewer). <br>
     * <br>
     * 
     * Coordinates are start masked to the scale of the texture being used and when
     * we reverse an orthogonalAxis, we use the texture's sliceMask as the basis so
     * that we remain within the frame of the texture scale we are using. <br>
     * <br>
     * 
     * Note that the x, y components are for determining min/max UV values. They
     * should NOT be used to set vertex UV coordinates directly. All bigtex models
     * should have lockUV = true, which means that uv coordinates will be derived at
     * time of quad bake by projecting each vertex onto the plane of the quad's
     * nominal face. Setting UV coordinates on a quad with lockUV=true has no
     * effect.
     */
    protected static Vec3i getSurfaceVector(int blockX, int blockY, int blockZ, Direction face, TextureScale scale) {
        // PERF: reuse instances?

        int sliceCountMask = scale.sliceCountMask;
        int x = blockX & sliceCountMask;
        int y = blockY & sliceCountMask;
        int z = blockZ & sliceCountMask;

        switch (face) {
        case EAST:
            return new Vec3i(sliceCountMask - z, sliceCountMask - y, -blockX);

        case WEST:
            return new Vec3i(z, sliceCountMask - y, blockX);

        case NORTH:
            return new Vec3i(sliceCountMask - x, sliceCountMask - y, blockZ);

        case SOUTH:
            return new Vec3i(x, sliceCountMask - y, -blockZ);

        case DOWN:
            return new Vec3i(x, sliceCountMask - z, blockY);

        case UP:
        default:
            return new Vec3i(x, z, -blockY);
        }
    }

    /**
     * Rotates given surface vector around the center of the texture by the given
     * degree.
     * 
     */
    protected static Vec3i rotateFacePerspective(Vec3i vec, Rotation rotation, TextureScale scale) {
        // PERF - reuse instances?
        switch (rotation) {
        case ROTATE_90:
            return new Vec3i(vec.getY(), scale.sliceCountMask - vec.getX(), vec.getZ());

        case ROTATE_180:
            return new Vec3i(scale.sliceCountMask - vec.getX(), scale.sliceCountMask - vec.getY(), vec.getZ());

        case ROTATE_270:
            return new Vec3i(scale.sliceCountMask - vec.getY(), vec.getX(), vec.getZ());

        case ROTATE_NONE:
        default:
            return vec;

        }
    }

    protected static int textureVersionForFace(Direction face, TextureSet tex, PrimitiveModelState modelState) {
        if (tex.versionCount() == 0)
            return 0;
        return textureHashForFace(face, tex, modelState) & tex.versionMask();
    }

    protected static int textureHashForFace(Direction face, TextureSet tex, PrimitiveModelState modelState) {
        final int species = modelState.hasSpecies() ? modelState.species() : 0;
        final int speciesBits = species << 16;
        final int shift = tex.scale().power;

        switch (face) {
        case DOWN:
        case UP: {
            final int yBits = (((modelState.posX() >> shift) & 0xFF) << 8) | ((modelState.posZ() >> shift) & 0xFF) | speciesBits;
            return HashCommon.mix(yBits);
        }

        case EAST:
        case WEST: {
            final int xBits = (((modelState.posY() >> shift) & 0xFF) << 8) | ((modelState.posZ() >> shift) & 0xFF) | speciesBits;
            return HashCommon.mix(xBits);
        }

        case NORTH:
        case SOUTH: {
            final int zBits = (((modelState.posX() >> shift) & 0xFF) << 8) | ((modelState.posY() >> shift) & 0xFF) | speciesBits;
            return HashCommon.mix(zBits);
        }

        default:
            return 0;
        }
    }

    /**
     * Gives randomized (if applicable) texture rotation for the given face. If
     * texture rotation type is FIXED, gives the textures default rotation. If
     * texture rotation type is CONSISTENT, is based on species only. If texture
     * rotation type is RANDOM, is based on position (chunked by texture size) and
     * species (if applies).
     */
    protected static Rotation textureRotationForFace(Direction face, TextureSet tex, PrimitiveModelState modelState) {
        final int species = modelState.hasSpecies() ? modelState.species() : 0;
        if (tex.rotation() == TextureRotation.ROTATE_RANDOM) {
            if (tex.scale() == TextureScale.SINGLE) {
                return Useful.offsetEnumValue(Rotation.ROTATE_NONE, (textureHashForFace(face, tex, modelState) >> 8) & 3);
            } else {
                return species == 0 ? Rotation.ROTATE_NONE : Useful.offsetEnumValue(Rotation.ROTATE_NONE, HashCommon.mix(species) & 3);
            }
        } else {
            return tex.rotation().rotation;
        }
    }
}