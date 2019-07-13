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

import grondag.fermion.color.ColorMap;
import grondag.fermion.color.ColorMap.EnumColorMap;
import grondag.fermion.serialization.IReadWriteNBT;
import grondag.fermion.serialization.PacketSerializable;
import grondag.xm2.block.XmBlockRegistryImpl.XmBlockStateImpl;
import grondag.xm2.connect.api.model.ClockwiseRotation;
import grondag.xm2.connect.api.state.CornerJoinState;
import grondag.xm2.connect.api.state.SimpleJoinState;
import grondag.xm2.mesh.BlockOrientationType;
import grondag.xm2.mesh.ModelShape;
import grondag.xm2.painting.PaintLayer;
import grondag.xm2.painting.VertexProcessor;
import grondag.xm2.primitives.PolyTransform;
import grondag.xm2.terrain.TerrainState;
import grondag.xm2.texture.api.TextureSet;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public interface ModelState extends IReadWriteNBT, PacketSerializable {
    boolean isImmutable();
    
    ImmutableModelState toImmutable();
    
    int[] serializeToInts();

    /**
     * Persisted but not part of hash nor included in equals comparison. If true,
     * refreshFromWorldState does nothing.
     */
    boolean isStatic();

    void setStatic(boolean isStatic);

    boolean equalsIncludeStatic(Object obj);

    @Override
    int hashCode();

    /** returns self as convenience method */
    ModelState refreshFromWorld(XmBlockStateImpl state, BlockView world, BlockPos pos);

    ModelShape<?> getShape();

    /**
     * Also resets shape-specific bits to default for the given shape. Does nothing
     * if shape is the same as existing.
     */
    void setShape(ModelShape<?> shape);

    int getColorARGB(PaintLayer layer);

    void setColorRGB(PaintLayer layer, int rgb);

    /**
     * See {@link #setAlpha(PaintLayer, int)}
     */
    int getAlpha(PaintLayer layer);

    /**
     * 255 (default) = fully opaque, 0 = invisible. Determines alpha component of
     * {@link #getColorARGB(PaintLayer)} Only applies if
     * {@link #isTranslucent(PaintLayer)} is true.
     */
    void setAlpha(PaintLayer layer, int translucency);

    /**
     * Used by placement logic to know if shape has any kind of orientation to it
     * that can be selected during placement.
     */
    BlockOrientationType orientationType();

    Direction.Axis getAxis();

    void setAxis(Direction.Axis axis);

    boolean isAxisInverted();

    void setAxisInverted(boolean isInverted);

    /**
     * For base/lamp paint layers, true means should be rendered in translucent
     * render layer. (Overlay textures always render in translucent layer.) For all
     * paint layers, true also means {@link #getAlpha(PaintLayer)} applies.
     */
    boolean isTranslucent(PaintLayer layer);

    /**
     * See {@link #isTranslucent(PaintLayer)}
     */
    void setTranslucent(PaintLayer layer, boolean isTranslucent);

    /**
     * Will be true if layer is assigned a texture.
     */
    boolean isLayerEnabled(PaintLayer l);

    /**
     * Equivalent to setting the texture for the layer to
     * {@link TexturePaletteRegistry#NONE_ID}
     */
    void disableLayer(PaintLayer l);

    TextureSet getTexture(PaintLayer layer);

    void setTexture(PaintLayer layer, TextureSet tex);

    boolean isEmissive(PaintLayer layer);

    void setEmissive(PaintLayer layer, boolean isEmissive);

    int getPosX();

    void setPosX(int index);

    int getPosY();

    void setPosY(int index);

    int getPosZ();

    void setPosZ(int index);

    /**
     * Usage is determined by shape. Limited to 44 bits and does not update from
     * world.
     */
    long getStaticShapeBits();

    /** usage is determined by shape */
    void setStaticShapeBits(long bits);

    /**
     * Will return 0 if model state does not include species. This is more
     * convenient than checking each place species is used.
     * 
     * @return
     */
    int getSpecies();

    void setSpecies(int species);

    CornerJoinState getCornerJoin();

    void setCornerJoin(CornerJoinState join);

    SimpleJoinState getSimpleJoin();

    void setSimpleJoin(SimpleJoinState join);

    SimpleJoinState getMasonryJoin();

    void setMasonryJoin(SimpleJoinState join);

    /**
     * For machines and other blocks with a privileged horizontal face, North is
     * considered the zero rotation.
     */
    ClockwiseRotation getAxisRotation();

    /**
     * For machines and other blocks with a privileged horizontal face, North is
     * considered the zero rotation.
     */
    void setAxisRotation(ClockwiseRotation rotation);

    /**
     * Multiblock shapes also get a full 64 bits of information - does not update
     * from world
     */
    long getMultiBlockBits();

    /**
     * Multiblock shapes also get a full 64 bits of information - does not update
     * from world
     */
    void setMultiBlockBits(long bits);

    TerrainState getTerrainState();

    long getTerrainStateKey();

    int getTerrainHotness();

    void setTerrainState(TerrainState flowState);

    void setTerrainStateKey(long terrainStateKey);

    /**
     * Determines what rendering path should apply for the given paint layer based
     * on user choices and the constraints imposed by MC rendering.
     */
    BlockRenderLayer getRenderPass(PaintLayer layer);

    boolean hasAxis();

    boolean hasAxisOrientation();

    /**
     * True if shape mesh generator can output lamp surface quads.
     */
    boolean hasLampSurface();

    /**
     * True if base paint layer is translucent or lamp paint layer is present and
     * translucent.
     */
    boolean hasTranslucentGeometry();

    boolean hasAxisRotation();

    boolean hasMasonryJoin();

    boolean hasTextureRotation();

    /**
     * Means that one or more elements (like a texture) uses species. Does not mean
     * that the shape or block actually capture or generate species other than 0.
     */
    boolean hasSpecies();

    /** Convenience method. Same as shape attribute. */
    MetaUsage metaUsage();

    /** Convenience method. Same as shape attribute. */
    boolean isAxisOrthogonalToPlacementFace();

    /**
     * Retrieves block/item metadata that should apply to this modelState.
     */
    int getMetaData();

    void setMetaData(int meta);

    /** True if shape can be placed on itself to grow */
    boolean isAdditive();

    /** returns true if geometry is a full 1x1x1 cube. */
    boolean isCube();

    /**
     * How much of the sky is occluded by the shape of this block? Based on geometry
     * alone, not transparency. Returns 0 if no occlusion (unlikely result). 1-15 if
     * some occlusion. 255 if fully occludes sky.
     */
    int geometricSkyOcclusion();

    /**
     * Returns true if visual elements and geometry match. Does not consider species
     * in matching.
     */
    boolean doShapeAndAppearanceMatch(ModelState other);

    /**
     * Returns true if visual elements match. Does not consider species or geometry
     * in matching.
     */
    boolean doesAppearanceMatch(ModelState other);

    /**
     * Returns a copy of this model state with only the bits that matter for
     * geometry. Used as lookup key for block damage models.
     */
    ModelState geometricState();

    /**
     * See {@link PolyTransform#rotateFace(ModelState, Direction)}
     */
    Direction rotateFace(Direction face);

    ModelState clone();

    /**
     * For backwards compatibility with color maps
     */
    @Deprecated
    public default void setColorMap(PaintLayer layer, ColorMap colorMap) {
        this.setColorRGB(layer, colorMap.getColor(EnumColorMap.BASE));
    }

    public void setVertexProcessor(PaintLayer layer, VertexProcessor vp);

    public VertexProcessor getVertexProcessor(PaintLayer layer);
}
