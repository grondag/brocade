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

import grondag.fermion.intstream.IntStream;
import grondag.fermion.intstream.IntStreams;
import grondag.xm.api.mesh.ReadOnlyMesh;
import grondag.xm.api.mesh.WritableMesh;
import grondag.xm.api.mesh.polygon.MutablePolygon;
import grondag.xm.api.mesh.polygon.Polygon;

@API(status = INTERNAL)
class WritableMeshImpl extends AbstractXmMesh implements WritableMesh {
    private static final int MAX_STRIDE;

    static {
        final int maxFormat = MeshFormat.MUTABLE_FLAG | MeshFormat.HAS_LINK_FLAG | MeshFormat.HAS_TAG_FLAG;

        MAX_STRIDE = 1 + StaticEncoder.INTEGER_WIDTH + PolyEncoder.get(maxFormat).stride();
    }

    protected final StreamBackedMutablePolygon writer;
    protected final StreamBackedPolygon copyFrom = new StreamBackedPolygon();

    protected IntStream writerStream;
    protected IntStream defaultStream;

    /**
     * Format flags used for the writer polygon. Always includes mutable flag. Also
     * the main stream format for mutable subclass.
     */
    protected int formatFlags = 0;

    public WritableMeshImpl() {
        writer = new StreamBackedMutablePolygon();
        // note this never changes - writer stream is dedicated,
        // and we already write at the start of it
        writer.baseAddress = 0;
    }

    @Override
    public void clear() {
        stream.clear();
        originAddress = newOrigin();
        writeAddress = originAddress;
        // force error on read
        reader.invalidate();
        polyA.invalidate();
        polyB.invalidate();
        internal.invalidate();
        clearDefaults();
        loadDefaults();
    }

    protected final void prepare(IntStream stream, int formatFlags) {
        super.prepare(stream);
        copyFrom.stream = stream;
        defaultStream = IntStreams.claim();
        writerStream = IntStreams.claim();
        writer.stream = writerStream;
        this.formatFlags = formatFlags | MeshFormat.MUTABLE_FLAG;
        clearDefaults();
        loadDefaults();
    }

    protected void prepare(int formatFlags) {
        prepare(IntStreams.claim(), formatFlags);
    }

    @Override
    protected final void prepare(IntStream stream) {
        prepare(stream, 0);
    }

    @Override
    protected void doRelease() {
        super.doRelease();
        copyFrom.stream = null;
        defaultStream.release();
        writerStream.release();
        defaultStream = null;
        writerStream = null;
    }

    @Override
    protected void returnToPool() {
        XmMeshesImpl.release(this);
    }

    @Override
    public MutablePolygon writer() {
        return writer;
    }

    @Override
    public int writerAddress() {
        return writeAddress;
    }

    @Override
    public final void append() {
        appendCopy(writer, formatFlags);
        loadDefaults();
    }

    @Override
    public void saveDefaults() {
        writer.clearFaceNormal();
        defaultStream.clear();
        defaultStream.copyFrom(0, writerStream, 0, MeshFormat.polyStride(writer.format(), false));
    }

    @Override
    public void clearDefaults() {
        defaultStream.clear();
        defaultStream.set(0, MeshFormat.setVertexCount(formatFlags, 4));
        writer.stream = defaultStream;
        writer.loadFormat();

        writer.loadStandardDefaults();

        writer.stream = writerStream;
        writer.loadFormat();
    }

    @Override
    public void loadDefaults() {
        writerStream.clear();
        writerStream.copyFrom(0, defaultStream, 0, MAX_STRIDE);
        writer.loadFormat();
    }

    @Override
    public ReadOnlyMesh releaseAndConvertToReader(int formatFlags) {
        ReadOnlyMesh result = XmMeshesImpl.claimReadOnly(this, formatFlags);
        release();
        return result;
    }

    @Override
    public void setVertexCount(int vertexCount) {
        writer.setFormat(MeshFormat.setVertexCount(writer.format(), vertexCount));
    }

    @Override
    public void setLayerCount(int layerCount) {
        writer.spriteDepth(layerCount);
    }

    @Override
    public void appendCopy(Polygon poly) {
        appendCopy(poly, formatFlags);
    }

    @Override
    public int splitIfNeeded(int targetAddress) {
        internal.moveTo(targetAddress);
        final int inCount = internal.vertexCount();
        if (inCount == 3 || (inCount == 4 && internal.isConvex()))
            return Polygon.NO_LINK_OR_TAG;

        int firstSplitAddress = this.writerAddress();

        int head = inCount - 1;
        int tail = 2;
        setVertexCount(4);
        writer.copyFrom(internal, false);
        writer.copyVertexFrom(0, internal, head);
        writer.copyVertexFrom(1, internal, 0);
        writer.copyVertexFrom(2, internal, 1);
        writer.copyVertexFrom(3, internal, tail);
        append();

        while (head - tail > 1) {
            final int vCount = head - tail == 2 ? 3 : 4;
            setVertexCount(vCount);
            writer.copyFrom(internal, false);
            writer.copyVertexFrom(0, internal, head);
            writer.copyVertexFrom(1, internal, tail);
            writer.copyVertexFrom(2, internal, ++tail);
            if (vCount == 4) {
                writer.copyVertexFrom(3, internal, --head);

                // if we end up with a convex quad
                // backtrack and output a tri instead
                if (!writer.isConvex()) {
                    head++;
                    tail--;
                    setVertexCount(3);
                    writer.copyFrom(internal, false);
                    writer.copyVertexFrom(0, internal, head);
                    writer.copyVertexFrom(1, internal, tail);
                    writer.copyVertexFrom(2, internal, ++tail);
                }
            }
            append();
        }
        internal.setDeleted();
        return firstSplitAddress;
    }
}