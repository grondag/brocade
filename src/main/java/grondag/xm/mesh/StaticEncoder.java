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
package grondag.xm.mesh;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

import grondag.fermion.bits.BitPacker32;
import grondag.fermion.intstream.IntStream;
import grondag.fermion.spatial.Rotation;
import grondag.fermion.varia.IndexedInterner;
import grondag.xm.api.primitive.surface.XmSurface;
import net.minecraft.block.BlockRenderLayer;

@API(status = INTERNAL)
class StaticEncoder {
    private static final BitPacker32<StaticEncoder> BITPACKER = new BitPacker32<StaticEncoder>(null, null);
    private static final BitPacker32<StaticEncoder> BITPACKER_2 = new BitPacker32<StaticEncoder>(null, null);
    
    private static final IndexedInterner<XmSurface> xmSurfaces = new IndexedInterner<>(XmSurface.class);

    private static final int BIT_OFFSET = 1;
    private static final int BIT_OFFSET_2 = 2;
    private static final int TEXTURE_PIPELINE_OFFSET = 3;
    private static final int SURFACE_OFFSET = 4;
    private static final int UV_WRAP_DIST_OFFSET = 5;

    /**
     * How many integers in the stream are needed for static encoding. This is in
     * addition to the format header.
     */
    public static final int INTEGER_WIDTH = 5;

    public static XmSurface surface(IntStream stream, int baseAddress) {
        return xmSurfaces.fromHandle(stream.get(baseAddress + SURFACE_OFFSET));
    }

    public static void surface(IntStream stream, int baseAddress, XmSurface surface) {
        stream.set(baseAddress + SURFACE_OFFSET, xmSurfaces.toHandle(surface));
    }

    public static float uvWrapDistance(IntStream stream, int baseAddress) {
        return Float.intBitsToFloat(stream.get(baseAddress + UV_WRAP_DIST_OFFSET));
    }

    public static void uvWrapDistance(IntStream stream, int baseAddress, float uvWrapDistance) {
        stream.set(baseAddress + UV_WRAP_DIST_OFFSET, Float.floatToRawIntBits(uvWrapDistance));
    }

    public static int getPipelineIndex(IntStream stream, int baseAddress) {
        return stream.get(baseAddress + TEXTURE_PIPELINE_OFFSET) >>> 16;
    }

    public static void setPipelineIndex(IntStream stream, int baseAddress, int pipelineIndex) {
        final int surfaceVal = stream.get(baseAddress + TEXTURE_PIPELINE_OFFSET) & 0x0000FFFF;
        stream.set(baseAddress + TEXTURE_PIPELINE_OFFSET, surfaceVal | (pipelineIndex << 16));
    }

    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.BooleanElement[] CONTRACT_UV = (BitPacker32<StaticEncoder>.BooleanElement[]) new BitPacker32<?>.BooleanElement[3];

    static {
        CONTRACT_UV[0] = BITPACKER.createBooleanElement();
        CONTRACT_UV[1] = BITPACKER.createBooleanElement();
        CONTRACT_UV[2] = BITPACKER.createBooleanElement();
    }

    public static boolean shouldContractUVs(IntStream stream, int baseAddress, int layerIndex) {
        // want default to be true - easiest way is to flip here so that 0 bit gives
        // right default
        return !CONTRACT_UV[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void setContractUVs(IntStream stream, int baseAddress, int layerIndex, boolean shouldContract) {
        // want default to be true - easiest way is to flip here so that 0 bit gives
        // right default
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, CONTRACT_UV[layerIndex].setValue(!shouldContract, bits));
    }

    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.EnumElement<Rotation>[] ROTATION = (BitPacker32<StaticEncoder>.EnumElement<Rotation>[]) new BitPacker32<?>.EnumElement<?>[3];

    static {
        ROTATION[0] = BITPACKER.createEnumElement(Rotation.class);
        ROTATION[1] = BITPACKER.createEnumElement(Rotation.class);
        ROTATION[2] = BITPACKER.createEnumElement(Rotation.class);
    }

    public static Rotation getRotation(IntStream stream, int baseAddress, int layerIndex) {
        return ROTATION[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void setRotation(IntStream stream, int baseAddress, int layerIndex, Rotation rotation) {
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, ROTATION[layerIndex].setValue(rotation, bits));
    }

    private static final BitPacker32<StaticEncoder>.IntElement SALT = BITPACKER.createIntElement(256);

    public static int getTextureSalt(IntStream stream, int baseAddress) {
        return SALT.getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void setTextureSalt(IntStream stream, int baseAddress, int salt) {
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, SALT.setValue(salt, bits));
    }

    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.BooleanElement[] LOCK_UV = (BitPacker32<StaticEncoder>.BooleanElement[]) new BitPacker32<?>.BooleanElement[3];

    static {
        LOCK_UV[0] = BITPACKER_2.createBooleanElement();
        LOCK_UV[1] = BITPACKER_2.createBooleanElement();
        LOCK_UV[2] = BITPACKER_2.createBooleanElement();
    }

    public static boolean isLockUV(IntStream stream, int baseAddress, int layerIndex) {
        return LOCK_UV[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET_2));
    }

    public static void setLockUV(IntStream stream, int baseAddress, int layerIndex, boolean lockUV) {
        final int bits = stream.get(baseAddress + BIT_OFFSET_2);
        stream.set(baseAddress + BIT_OFFSET_2, LOCK_UV[layerIndex].setValue(lockUV, bits));
    }

    //PERF: improve LOR
    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.BooleanElement[] EMISSIVE = (BitPacker32<StaticEncoder>.BooleanElement[]) new BitPacker32<?>.BooleanElement[3];
    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.BooleanElement[] AO = (BitPacker32<StaticEncoder>.BooleanElement[]) new BitPacker32<?>.BooleanElement[3];
    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.BooleanElement[] DIFFUSE = (BitPacker32<StaticEncoder>.BooleanElement[]) new BitPacker32<?>.BooleanElement[3];

    static {
        EMISSIVE[0] = BITPACKER.createBooleanElement();
        EMISSIVE[1] = BITPACKER.createBooleanElement();
        EMISSIVE[2] = BITPACKER.createBooleanElement();
        AO[0] = BITPACKER.createBooleanElement();
        AO[1] = BITPACKER.createBooleanElement();
        AO[2] = BITPACKER.createBooleanElement();
        DIFFUSE[0] = BITPACKER.createBooleanElement();
        DIFFUSE[1] = BITPACKER.createBooleanElement();
        DIFFUSE[2] = BITPACKER.createBooleanElement();
    }

    public static boolean isEmissive(IntStream stream, int baseAddress, int layerIndex) {
        return EMISSIVE[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void setEmissive(IntStream stream, int baseAddress, int layerIndex, boolean disable) {
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, EMISSIVE[layerIndex].setValue(disable, bits));
    }

    public static boolean disableAo(IntStream stream, int baseAddress, int layerIndex) {
        return AO[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void disableAo(IntStream stream, int baseAddress, int layerIndex, boolean disable) {
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, AO[layerIndex].setValue(disable, bits));
    }
    
    public static boolean disableDiffuse(IntStream stream, int baseAddress, int layerIndex) {
        return DIFFUSE[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void disableDiffuse(IntStream stream, int baseAddress, int layerIndex, boolean isEmissive) {
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, DIFFUSE[layerIndex].setValue(isEmissive, bits));
    }
    
    @SuppressWarnings("unchecked")
    private static final BitPacker32<StaticEncoder>.EnumElement<BlockRenderLayer>[] RENDER_LAYER = (BitPacker32<StaticEncoder>.EnumElement<BlockRenderLayer>[]) new BitPacker32<?>.EnumElement<?>[3];

    static {
        RENDER_LAYER[0] = BITPACKER.createEnumElement(BlockRenderLayer.class);
        RENDER_LAYER[1] = BITPACKER.createEnumElement(BlockRenderLayer.class);
        RENDER_LAYER[2] = BITPACKER.createEnumElement(BlockRenderLayer.class);

        assert BITPACKER.bitLength() <= 32;
        assert BITPACKER_2.bitLength() <= 32;
    }

    public static BlockRenderLayer getRenderLayer(IntStream stream, int baseAddress, int layerIndex) {
        return RENDER_LAYER[layerIndex].getValue(stream.get(baseAddress + BIT_OFFSET));
    }

    public static void setRenderLayer(IntStream stream, int baseAddress, int layerIndex, BlockRenderLayer layer) {
        final int bits = stream.get(baseAddress + BIT_OFFSET);
        stream.set(baseAddress + BIT_OFFSET, RENDER_LAYER[layerIndex].setValue(layer, bits));
    }
}