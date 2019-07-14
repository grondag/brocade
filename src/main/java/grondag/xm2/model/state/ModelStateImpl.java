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

package grondag.xm2.model.state;

import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_HAS_AXIS_ROTATION;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_NEEDS_CORNER_JOIN;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_NEEDS_MASONRY_JOIN;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_NEEDS_POS;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_NEEDS_SIMPLE_JOIN;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_NEEDS_SPECIES;
import static grondag.xm2.model.state.ModelStateData.STATE_FLAG_NEEDS_TEXTURE_ROTATION;
import static grondag.xm2.model.state.ModelStateData.TEST_GETTER_STATIC;

import grondag.fermion.serialization.NBTDictionary;
import grondag.fermion.varia.Useful;
import grondag.xm2.Xm;
import grondag.xm2.XmConfig;
import grondag.xm2.api.connect.model.ClockwiseRotation;
import grondag.xm2.api.connect.state.CornerJoinState;
import grondag.xm2.api.connect.state.SimpleJoinState;
import grondag.xm2.api.connect.world.BlockNeighbors;
import grondag.xm2.api.model.ModelPrimitive;
import grondag.xm2.api.model.ModelPrimitiveRegistry;
import grondag.xm2.api.model.ModelState;
import grondag.xm2.api.model.MutableModelWorldState;
import grondag.xm2.api.model.MutablePrimitiveModelState;
import grondag.xm2.block.XmBlockRegistryImpl.XmBlockStateImpl;
import grondag.xm2.block.XmMasonryMatch;
import grondag.xm2.connect.CornerJoinStateSelector;
import grondag.xm2.mesh.helper.PolyTransform;
import grondag.xm2.model.ModelPrimitiveRegistryImpl;
import grondag.xm2.model.primitive.AbstractModelPrimitive;
import grondag.xm2.terrain.TerrainState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;

public class ModelStateImpl implements MutablePrimitiveModelState, MutableModelWorldState {
    private static final String NBT_MODEL_BITS = NBTDictionary.claim("modelState");
    private static final String NBT_SHAPE = NBTDictionary.claim("shape");
    /**
     * Stores string containing registry names of textures, vertex processors
     */
    private static final String NBT_LAYERS = NBTDictionary.claim("layers");

    /**
     * Removes model state from the tag if present.
     */
    public static final void clearNBTValues(CompoundTag tag) {
        if (tag == null)
            return;
        tag.remove(NBT_MODEL_BITS);
        tag.remove(NBT_SHAPE);
        tag.remove(NBT_LAYERS);
    }

    private boolean isStatic = false;
    protected long coreBits;
    protected long shapeBits1;
    protected long shapeBits0;

    // default to white, full alpha
    protected long layerBitsBase = 0xFFFFFFFFL;
    protected long layerBitsLamp = 0xFFFFFFFFL;
    protected long layerBitsMiddle = 0xFFFFFFFFL;
    protected long layerBitsOuter = 0xFFFFFFFFL;
    protected long layerBitsCut = 0xFFFFFFFFL;

    // TODO: replace mockup implementation
    protected int[] paints = new int[6];

    private int hashCode = -1;

    /** contains indicators derived from shape and painters */
    protected int stateFlags;

    public ModelStateImpl() {
    }

    public ModelStateImpl(int[] bits) {
        this.deserializeFromInts(bits);
    }

    public ModelStateImpl(ModelStateImpl template) {
        this.coreBits = template.coreBits;
        this.shapeBits0 = template.shapeBits0;
        this.shapeBits1 = template.shapeBits1;
        this.layerBitsBase = template.layerBitsBase;
        this.layerBitsCut = template.layerBitsCut;
        this.layerBitsLamp = template.layerBitsLamp;
        this.layerBitsMiddle = template.layerBitsMiddle;
        this.layerBitsOuter = template.layerBitsOuter;
        this.paints[0] = template.paints[0];
        this.paints[1] = template.paints[1];
        this.paints[2] = template.paints[2];
        this.paints[3] = template.paints[3];
        this.paints[4] = template.paints[4];
        this.paints[5] = template.paints[5];
    }

    @Override
    public ModelStateImpl mutableCopy() {
        return new ModelStateImpl(this);
    }

    @Override
    public final int[] serializeToInts() {
        int[] result = new int[16 + 6];
        result[0] = (int) (this.isStatic ? (coreBits >> 32) | Useful.INT_SIGN_BIT : (coreBits >> 32));
        result[1] = (int) (coreBits);

        result[2] = (int) (shapeBits1 >> 32);
        result[3] = (int) (shapeBits1);

        result[4] = (int) (shapeBits0 >> 32);
        result[5] = (int) (shapeBits0);

        result[6] = (int) (layerBitsBase >> 32);
        result[7] = (int) (layerBitsBase);

        result[8] = (int) (layerBitsCut >> 32);
        result[9] = (int) (layerBitsCut);

        result[10] = (int) (layerBitsLamp >> 32);
        result[11] = (int) (layerBitsLamp);

        result[12] = (int) (layerBitsMiddle >> 32);
        result[13] = (int) (layerBitsMiddle);

        result[14] = (int) (layerBitsOuter >> 32);
        result[15] = (int) (layerBitsOuter);

        result[16] = paints[0];
        result[17] = paints[1];
        result[18] = paints[2];
        result[19] = paints[3];
        result[20] = paints[4];
        result[21] = paints[5];

        return result;
    }

    /**
     * Note does not reset state flag - do that if calling on an existing instance.
     */
    private void deserializeFromInts(int[] bits) {
        // sign on first long word is used to store static indicator
        this.isStatic = (Useful.INT_SIGN_BIT & bits[0]) == Useful.INT_SIGN_BIT;

        this.coreBits = ((long) (Useful.INT_SIGN_BIT_INVERSE & bits[0])) << 32 | (bits[1] & 0xffffffffL);
        this.shapeBits1 = ((long) bits[2]) << 32 | (bits[3] & 0xffffffffL);
        this.shapeBits0 = ((long) bits[4]) << 32 | (bits[5] & 0xffffffffL);

        this.layerBitsBase = ((long) bits[6]) << 32 | (bits[7] & 0xffffffffL);
        this.layerBitsCut = ((long) bits[8]) << 32 | (bits[9] & 0xffffffffL);
        this.layerBitsLamp = ((long) bits[10]) << 32 | (bits[11] & 0xffffffffL);
        this.layerBitsMiddle = ((long) bits[12]) << 32 | (bits[13] & 0xffffffffL);
        this.layerBitsOuter = ((long) bits[14]) << 32 | (bits[15] & 0xffffffffL);

        paints[0] = bits[16];
        paints[1] = bits[17];
        paints[2] = bits[18];
        paints[3] = bits[19];
        paints[4] = bits[20];
        paints[5] = bits[21];
    }

    private void populateStateFlagsIfNeeded() {
        if (this.stateFlags == 0) {
            this.stateFlags = ModelStateFlagHelper.getFlags(this);
        }
    }

    private void clearStateFlags() {
        if (this.stateFlags != 0) {
            this.stateFlags = 0;
        }
    }

    @Override
    public boolean isStatic() {
        return this.isStatic;
    }

    @Override
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    /**
     * Does NOT consider isStatic in comparison. <br>
     * <br>
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof ModelStateImpl) {
            ModelStateImpl other = (ModelStateImpl) obj;
            return this.coreBits == other.coreBits
                    && this.shapeBits0 == other.shapeBits0
                    && this.shapeBits1 == other.shapeBits1
                    && this.layerBitsBase == other.layerBitsBase
                    && this.layerBitsCut == other.layerBitsCut
                    && this.layerBitsLamp == other.layerBitsLamp
                    && this.layerBitsMiddle == other.layerBitsMiddle
                    && this.layerBitsOuter == other.layerBitsOuter
                    && this.paints[0] == other.paints[0]
                    && this.paints[1] == other.paints[1]
                    && this.paints[2] == other.paints[2]
                    && this.paints[3] == other.paints[3]
                    && this.paints[4] == other.paints[4]
                    && this.paints[5] == other.paints[5];
        }

        return false;
    }

    @Override
    public boolean equalsIncludeStatic(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof ModelStateImpl) {
            ModelStateImpl other = (ModelStateImpl) obj;
            return this.isStatic == other.isStatic
                    && this.coreBits == other.coreBits
                    && this.shapeBits0 == other.shapeBits0
                    && this.shapeBits1 == other.shapeBits1
                    && this.layerBitsBase == other.layerBitsBase
                    && this.layerBitsCut == other.layerBitsCut
                    && this.layerBitsLamp == other.layerBitsLamp
                    && this.layerBitsMiddle == other.layerBitsMiddle
                    && this.layerBitsOuter == other.layerBitsOuter
                    && this.paints[0] == other.paints[0]
                    && this.paints[1] == other.paints[1]
                    && this.paints[2] == other.paints[2]
                    && this.paints[3] == other.paints[3]
                    && this.paints[4] == other.paints[4]
                    && this.paints[5] == other.paints[5];
        }
        return false;
    }

    private void invalidateHashCode() {
        if (this.hashCode != -1)
            this.hashCode = -1;
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = (int) Useful.longHash(this.coreBits ^ this.shapeBits0 ^ this.shapeBits1 ^ this.layerBitsBase
                    ^ this.layerBitsCut ^ this.layerBitsLamp ^ this.layerBitsMiddle ^ this.layerBitsOuter ^ paints[0]
                    ^ paints[1] ^ paints[2] ^ paints[3] ^ paints[4] ^ paints[5]);
        }
        return hashCode;
    }

    // TODO: remove - blocks should know how to refresh their state - state should
    // not be coupled
    @Deprecated
    @Override
    public ModelStateImpl refreshFromWorld(XmBlockStateImpl xmState, BlockView world, BlockPos pos) {

        // Output.getLog().info("ModelState.refreshFromWorld static=" + this.isStatic +
        // " @" + pos.toString());
        if (this.isStatic)
            return this;

        populateStateFlagsIfNeeded();

        switch (((AbstractModelPrimitive) primitive()).stateFormat) {
            case BLOCK:

                if ((stateFlags & STATE_FLAG_NEEDS_POS) == STATE_FLAG_NEEDS_POS)
                    refreshBlockPosFromWorld(pos, 255);

                BlockNeighbors neighbors = null;

                if ((STATE_FLAG_NEEDS_CORNER_JOIN & stateFlags) == STATE_FLAG_NEEDS_CORNER_JOIN) {
                    neighbors = BlockNeighbors.claim(world, pos, TEST_GETTER_STATIC, xmState.blockJoinTest());
                    ModelStateData.BLOCK_JOIN.setValue(CornerJoinState.fromWorld(neighbors).ordinal(), this);

                } else if ((STATE_FLAG_NEEDS_SIMPLE_JOIN & stateFlags) == STATE_FLAG_NEEDS_SIMPLE_JOIN) {
                    neighbors = BlockNeighbors.claim(world, pos, TEST_GETTER_STATIC, xmState.blockJoinTest());
                    ModelStateData.BLOCK_JOIN.setValue(SimpleJoinState.fromWorld(neighbors).ordinal(), this);
                }

                if ((STATE_FLAG_NEEDS_MASONRY_JOIN & stateFlags) == STATE_FLAG_NEEDS_MASONRY_JOIN) {
                    if (neighbors == null) {
                        neighbors = BlockNeighbors.claim(world, pos, TEST_GETTER_STATIC, XmMasonryMatch.INSTANCE);
                    } else {
                        neighbors.withTest(XmMasonryMatch.INSTANCE);
                    }
                    ModelStateData.MASONRY_JOIN.setValue(SimpleJoinState.fromWorld(neighbors).ordinal(), this);
                }

                if (neighbors != null) {
                    neighbors.release();
                }

                break;

            case FLOW:
                // terrain blocks need larger position space to drive texture randomization
                // because doesn't have per-block rotation or version
                if ((stateFlags & STATE_FLAG_NEEDS_POS) == STATE_FLAG_NEEDS_POS)
                    refreshBlockPosFromWorld(pos, 255);
                TerrainState.produceBitsFromWorldStatically(xmState.blockState, world, pos, (t, h) -> {
                    ModelStateData.FLOW_JOIN.setValue(t, this);
                    ModelStateData.EXTRA_SHAPE_BITS.setValue(h, this);
                    return null;
                });

                break;

            case MULTIBLOCK:
                break;

            default:
                break;

        }

        this.invalidateHashCode();

        return this;
    }

    /**
     * Saves world block pos relative to cube boundary specified by mask. Used by
     * BigTex surface painting for texture randomization on non-multiblock shapes.
     */
    private void refreshBlockPosFromWorld(BlockPos pos, int mask) {
        ModelStateData.POS_X.setValue((pos.getX() & mask), this);
        ModelStateData.POS_Y.setValue((pos.getY() & mask), this);
        ModelStateData.POS_Z.setValue((pos.getZ() & mask), this);
    }

    ////////////////////////////////////////////////////
    // PACKER 0 ATTRIBUTES (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public ModelPrimitive primitive() {
        return ModelPrimitiveRegistryImpl.INSTANCE.get(ModelStateData.SHAPE.getValue(this));
    }

    @Override
    public void primitive(ModelPrimitive shape) {
        if (shape.index() != ModelStateData.SHAPE.getValue(this)) {
            ModelStateData.SHAPE.setValue(shape.index(), this);
            shape.applyDefaultState(this);
            invalidateHashCode();
            clearStateFlags();
        }
    }

    @Override
    public Direction.Axis getAxis() {
        return ModelStateData.AXIS.getValue(this);
    }

    @Override
    public void setAxis(Direction.Axis axis) {
        ModelStateData.AXIS.setValue(axis, this);
        invalidateHashCode();
    }

    @Override
    public boolean isAxisInverted() {
        return ModelStateData.AXIS_INVERTED.getValue(this);
    }

    @Override
    public void setAxisInverted(boolean isInverted) {
        ModelStateData.AXIS_INVERTED.setValue(isInverted, this);
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    // PACKER 2 ATTRIBUTES (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public int posX() {
        return ModelStateData.POS_X.getValue(this);
    }

    @Override
    public void posX(int index) {
        ModelStateData.POS_X.setValue(index, this);
        invalidateHashCode();
    }

    @Override
    public int posY() {
        return ModelStateData.POS_Y.getValue(this);
    }

    @Override
    public void posY(int index) {
        ModelStateData.POS_Y.setValue(index, this);
        invalidateHashCode();
    }

    @Override
    public int posZ() {
        return ModelStateData.POS_Z.getValue(this);
    }

    @Override
    public void posZ(int index) {
        ModelStateData.POS_Z.setValue(index, this);
        invalidateHashCode();
    }

    @Override
    public long getStaticShapeBits() {
        return ModelStateData.EXTRA_SHAPE_BITS.getValue(this);
    }

    @Override
    public void setStaticShapeBits(long bits) {
        ModelStateData.EXTRA_SHAPE_BITS.setValue(bits, this);
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    // PACKER 3 ATTRIBUTES (BLOCK FORMAT)
    ////////////////////////////////////////////////////

    @Override
    public int species() {
        this.populateStateFlagsIfNeeded();

        if (XmConfig.BLOCKS.debugModelState
                && !this.hasSpecies())
            Xm.LOG.warn("getSpecies on model state does not apply for shape");

        return this.hasSpecies() ? ModelStateData.SPECIES.getValue(this) : 0;
    }

    @Override
    public void species(int species) {
        this.populateStateFlagsIfNeeded();

        if (XmConfig.BLOCKS.debugModelState
                && !this.hasSpecies())
            Xm.LOG.warn("setSpecies on model state does not apply for shape");

        if (this.hasSpecies()) {
            ModelStateData.SPECIES.setValue(species, this);
            invalidateHashCode();
        }
    }

    @Override
    public CornerJoinState cornerJoin() {
        if (XmConfig.BLOCKS.debugModelState) {
            populateStateFlagsIfNeeded();
            if ((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0
                    || ((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK)
                Xm.LOG.warn("getCornerJoin on model state does not apply for shape");
        }

        return CornerJoinStateSelector.fromOrdinal(
                MathHelper.clamp(ModelStateData.BLOCK_JOIN.getValue(this), 0, CornerJoinState.STATE_COUNT - 1));
    }

    @Override
    public void cornerJoin(CornerJoinState join) {
        if (XmConfig.BLOCKS.debugModelState) {
            populateStateFlagsIfNeeded();
            if ((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0
                    || ((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK)
                Xm.LOG.warn("setCornerJoin on model state does not apply for shape");
        }

        ModelStateData.BLOCK_JOIN.setValue(join.ordinal(), this);
        invalidateHashCode();
    }

    @Override
    public SimpleJoinState simpleJoin() {
        if (XmConfig.BLOCKS.debugModelState
                && ((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK)
            Xm.LOG.warn("getSimpleJoin on model state does not apply for shape");

        // If this state is using corner join, join index is for a corner join
        // and so need to derive simple join from the corner join
        populateStateFlagsIfNeeded();
        return ((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0)
                ? SimpleJoinState.fromOrdinal(ModelStateData.BLOCK_JOIN.getValue(this))
                : cornerJoin().simpleJoin();
    }

    @Override
    public void simpleJoin(SimpleJoinState join) {
        if (XmConfig.BLOCKS.debugModelState) {
            if (((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK) {
                Xm.LOG.warn("Ignored setSimpleJoin on model state that does not apply for shape");
                return;
            }

            populateStateFlagsIfNeeded();
            if ((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) != 0) {
                Xm.LOG.warn("Ignored setSimpleJoin on model state that uses corner join instead");
                return;
            }
        }

        ModelStateData.BLOCK_JOIN.setValue(join.ordinal(), this);
        invalidateHashCode();
    }

    @Override
    public SimpleJoinState masonryJoin() {
        if (XmConfig.BLOCKS.debugModelState
                && (((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK
                        || (stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0)
                || ((stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == 0))
            Xm.LOG.warn("getMasonryJoin on model state does not apply for shape");

        populateStateFlagsIfNeeded();
        return SimpleJoinState.fromOrdinal(ModelStateData.MASONRY_JOIN.getValue(this));
    }

    @Override
    public void masonryJoin(SimpleJoinState join) {
        if (XmConfig.BLOCKS.debugModelState) {
            populateStateFlagsIfNeeded();
            if (((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK) {
                Xm.LOG.warn("Ignored setMasonryJoin on model state that does not apply for shape");
                return;
            }

            if (((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0)
                    || ((stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == 0)) {
                Xm.LOG.warn("Ignored setMasonryJoin on model state for which it does not apply");
                return;
            }
        }

        ModelStateData.MASONRY_JOIN.setValue(join.ordinal(), this);
        invalidateHashCode();
    }

    @Override
    public ClockwiseRotation getAxisRotation() {
        return ModelStateData.AXIS_ROTATION.getValue(this);
    }

    @Override
    public void setAxisRotation(ClockwiseRotation rotation) {
        populateStateFlagsIfNeeded();
        if (((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.BLOCK) {
            if (XmConfig.BLOCKS.debugModelState)
                Xm.LOG.warn("Ignored setAxisRotation on model state that does not apply for shape");
            return;
        }

        if ((stateFlags & STATE_FLAG_HAS_AXIS_ROTATION) == 0) {
            if (XmConfig.BLOCKS.debugModelState)
                Xm.LOG.warn("Ignored setAxisRotation on model state for which it does not apply");
            return;
        }

        ModelStateData.AXIS_ROTATION.setValue(rotation, this);
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    // PACKER 3 ATTRIBUTES (MULTI-BLOCK FORMAT)
    ////////////////////////////////////////////////////

    @Override
    public long getMultiBlockBits() {
        if (XmConfig.BLOCKS.debugModelState
                && ((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.MULTIBLOCK)
            Xm.LOG.warn("getMultiBlockBits on model state does not apply for shape");

        return shapeBits0;
    }

    @Override
    public void setMultiBlockBits(long bits) {
        if (XmConfig.BLOCKS.debugModelState
                && ((AbstractModelPrimitive) primitive()).stateFormat != StateFormat.MULTIBLOCK)
            Xm.LOG.warn("setMultiBlockBits on model state does not apply for shape");

        shapeBits0 = bits;
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    // PACKER 3 ATTRIBUTES (FLOWING TERRAIN FORMAT)
    ////////////////////////////////////////////////////

    @Override
    public long getTerrainStateKey() {
        assert ((AbstractModelPrimitive) primitive()).stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        return ModelStateData.FLOW_JOIN.getValue(this);
    }

    @Override
    public int getTerrainHotness() {
        assert ((AbstractModelPrimitive) primitive()).stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        return (int) ModelStateData.EXTRA_SHAPE_BITS.getValue(this);
    }

    @Override
    public void setTerrainStateKey(long terrainStateKey) {
        assert ((AbstractModelPrimitive) primitive()).stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        ModelStateData.FLOW_JOIN.setValue(terrainStateKey, this);
    }

    @Override
    public TerrainState getTerrainState() {
        assert ((AbstractModelPrimitive) primitive()).stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        return new TerrainState(ModelStateData.FLOW_JOIN.getValue(this),
                (int) ModelStateData.EXTRA_SHAPE_BITS.getValue(this));
    }

    @Override
    public void setTerrainState(TerrainState flowState) {
        assert ((AbstractModelPrimitive) primitive()).stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        ModelStateData.FLOW_JOIN.setValue(flowState.getStateKey(), this);
        ModelStateData.EXTRA_SHAPE_BITS.setValue(flowState.getHotness(), this);
        invalidateHashCode();
    }

    @Override
    public boolean hasTranslucentGeometry() {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY) == STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY;
    }

    @Override
    public boolean hasMasonryJoin() {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == STATE_FLAG_NEEDS_MASONRY_JOIN;
    }

    @Override
    public boolean hasTextureRotation() {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_NEEDS_TEXTURE_ROTATION) == STATE_FLAG_NEEDS_TEXTURE_ROTATION;
    }

    @Override
    public boolean hasSpecies() {
        this.populateStateFlagsIfNeeded();
        return ((this.stateFlags & STATE_FLAG_NEEDS_SPECIES) == STATE_FLAG_NEEDS_SPECIES);
    }

    @Override
    public final boolean doShapeAndAppearanceMatch(ModelState other) {
        final ModelStateImpl o = (ModelStateImpl) other;
        return (this.coreBits & ModelStateData.SHAPE_COMPARISON_MASK_0) == (o.coreBits
                & ModelStateData.SHAPE_COMPARISON_MASK_0)
                && (this.shapeBits1 & ModelStateData.SHAPE_COMPARISON_MASK_1) == (o.shapeBits1
                        & ModelStateData.SHAPE_COMPARISON_MASK_1)
                && this.layerBitsBase == o.layerBitsBase
                && this.layerBitsCut == o.layerBitsCut
                && this.layerBitsLamp == o.layerBitsLamp
                && this.layerBitsMiddle == o.layerBitsMiddle
                && this.layerBitsOuter == o.layerBitsOuter;
    }

    @Override
    public boolean doesAppearanceMatch(ModelState other) {
        final ModelStateImpl o = (ModelStateImpl) other;
        return this.layerBitsBase == o.layerBitsBase
                && this.layerBitsCut == o.layerBitsCut
                && this.layerBitsLamp == o.layerBitsLamp
                && this.layerBitsMiddle == o.layerBitsMiddle
                && this.layerBitsOuter == o.layerBitsOuter;
    }

    // PERF: bottleneck for Pyroclasm
    @Override
    public ModelStateImpl geometricState() {
        this.populateStateFlagsIfNeeded();
        ModelStateImpl result = new ModelStateImpl();
        result.primitive(this.primitive());

        switch (((AbstractModelPrimitive) primitive()).stateFormat) {
            case BLOCK:
                result.setStaticShapeBits(this.getStaticShapeBits());
                if (this.hasAxis())
                    result.setAxis(this.getAxis());
                if (this.hasAxisOrientation())
                    result.setAxisInverted(this.isAxisInverted());
                if (this.hasAxisRotation())
                    result.setAxisRotation(this.getAxisRotation());
                if ((this.primitive().stateFlags(this) & STATE_FLAG_NEEDS_CORNER_JOIN) == STATE_FLAG_NEEDS_CORNER_JOIN) {
                    result.cornerJoin(this.cornerJoin());
                } else if ((this.primitive().stateFlags(this)
                        & STATE_FLAG_NEEDS_SIMPLE_JOIN) == STATE_FLAG_NEEDS_SIMPLE_JOIN) {
                    result.simpleJoin(this.simpleJoin());
                }
                break;

            case FLOW:
                ModelStateData.FLOW_JOIN.setValue(ModelStateData.FLOW_JOIN.getValue(this), result);
                break;

            case MULTIBLOCK:
                result.shapeBits0 = this.shapeBits0;
                break;

            default:
                break;

        }
        return result;
    }

    public static ModelStateImpl deserializeFromNBTIfPresent(CompoundTag tag) {
        if (tag.containsKey(NBT_MODEL_BITS)) {
            ModelStateImpl result = new ModelStateImpl();
            result.deserializeNBT(tag);
            return result;
        }
        return null;
    }

    @Override
    public Direction rotateFace(Direction face) {
        return PolyTransform.rotateFace(this, face);
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag == null)
            return;

        int[] stateBits = tag.getIntArray(NBT_MODEL_BITS);
        if (stateBits.length != 22) {
            Xm.LOG.warn("Bad or missing data encounter during ModelState NBT deserialization.");
            return;
        }
        this.deserializeFromInts(stateBits);

        // shape is serialized by name because registered shapes can change if
        // mods/config change
        ModelPrimitive shape = ModelPrimitiveRegistry.INSTANCE.get(tag.getString(NBT_SHAPE));
        if (shape != null)
            ModelStateData.SHAPE.setValue(shape.index(), this);

        // textures and vertex processors serialized by name because registered can
        // change if mods/config change
//        String layers = tag.getString(NBT_LAYERS);
//        if (layers.isEmpty()) {
//            String[] names = layers.split(",");
//            if (names.length != 0) {
//                int i = 0;
//                for (PaintLayer l : PaintLayer.VALUES) {
//                    if (ModelStateData.PAINT_TEXTURE[l.ordinal()].getValue(this) != 0) {
//                        TextureSet tex = TextureSetRegistryImpl.INSTANCE.getById(new Identifier(names[i++]));
//                        ModelStateData.PAINT_TEXTURE[l.ordinal()].setValue(tex.index(), this);
//                        if (i == names.length)
//                            break;
//                    }
//
//                    if (ModelStateData.PAINT_VERTEX_PROCESSOR[l.ordinal()].getValue(this) != 0) {
//                        VertexProcessor vp = VertexProcessors.get(names[i++]);
//                        ModelStateData.PAINT_VERTEX_PROCESSOR[l.ordinal()].setValue(vp.ordinal, this);
//                        if (i == names.length)
//                            break;
//                    }
//                }
//            }
//        }
        this.clearStateFlags();
    }

    @Override
    public void serializeNBT(CompoundTag tag) {
        tag.putIntArray(NBT_MODEL_BITS, this.serializeToInts());

        // shape is serialized by name because registered shapes can change if
        // mods/config change
        tag.putString(NBT_SHAPE, this.primitive().id().toString());

        // TODO: serialization for paint/surface map
        // textures and vertex processors serialized by name because registered can
        // change if mods/config change
//        StringBuilder layers = new StringBuilder();
//       
//        if (layers.length() != 0)
//            tag.putString(NBT_LAYERS, layers.toString());
    }

    @Override
    public void fromBytes(PacketByteBuf pBuff) {
        this.coreBits = pBuff.readLong();
        this.shapeBits0 = pBuff.readLong();
        this.shapeBits1 = pBuff.readVarLong();
        this.layerBitsBase = pBuff.readVarLong();
        this.layerBitsCut = pBuff.readVarLong();
        this.layerBitsLamp = pBuff.readVarLong();
        this.layerBitsMiddle = pBuff.readVarLong();
        this.layerBitsOuter = pBuff.readVarLong();
        this.paints[0] = pBuff.readVarInt();
        this.paints[1] = pBuff.readVarInt();
        this.paints[2] = pBuff.readVarInt();
        this.paints[3] = pBuff.readVarInt();
        this.paints[4] = pBuff.readVarInt();
        this.paints[5] = pBuff.readVarInt();
    }

    @Override
    public void toBytes(PacketByteBuf pBuff) {
        pBuff.writeLong(this.coreBits);
        pBuff.writeLong(this.shapeBits0);
        pBuff.writeVarLong(this.shapeBits1);
        pBuff.writeVarLong(this.layerBitsBase);
        pBuff.writeVarLong(this.layerBitsCut);
        pBuff.writeVarLong(this.layerBitsLamp);
        pBuff.writeVarLong(this.layerBitsMiddle);
        pBuff.writeVarLong(this.layerBitsOuter);
        pBuff.writeVarInt(paints[0]);
        pBuff.writeVarInt(paints[1]);
        pBuff.writeVarInt(paints[2]);
        pBuff.writeVarInt(paints[3]);
        pBuff.writeVarInt(paints[4]);
        pBuff.writeVarInt(paints[5]);
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public ImmutableModelStateImpl toImmutable() {
        return new ImmutableModelStateImpl(this);
    }

    @Override
    public void paint(int surfaceIndex, int paintIndex) {
        paints[surfaceIndex] = paintIndex;
    }

    @Override
    public int paintIndex(int surfaceIndex) {
        return paints[surfaceIndex];
    }

    @Override
    public MutableModelWorldState worldState() {
        return this;
    }
}
