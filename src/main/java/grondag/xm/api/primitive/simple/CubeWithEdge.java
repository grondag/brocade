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
package grondag.xm.api.primitive.simple;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.function.Function;

import org.apiguardian.api.API;

import grondag.fermion.spatial.Rotation;
import grondag.xm.Xm;
import grondag.xm.api.mesh.WritableMesh;
import grondag.xm.api.mesh.XmMesh;
import grondag.xm.api.mesh.XmMeshes;
import grondag.xm.api.mesh.polygon.MutablePolygon;
import grondag.xm.api.mesh.polygon.PolyTransform;
import grondag.xm.api.orientation.OrientationType;
import grondag.xm.api.paint.SurfaceTopology;
import grondag.xm.api.primitive.SimplePrimitive;
import grondag.xm.api.primitive.surface.XmSurface;
import grondag.xm.api.primitive.surface.XmSurfaceList;
import net.minecraft.util.math.Direction;

@API(status = EXPERIMENTAL)
public class CubeWithEdge  {
    public static final XmSurfaceList SURFACES = XmSurfaceList.builder()
            .add("front", SurfaceTopology.CUBIC, XmSurface.FLAG_ALLOW_BORDERS)
            .add("back", SurfaceTopology.CUBIC, XmSurface.FLAG_ALLOW_BORDERS)
            .add("side", SurfaceTopology.CUBIC, XmSurface.FLAG_ALLOW_BORDERS)
            .build();

    public static final XmSurface SURFACE_FRONT = SURFACES.get(0);
    public static final XmSurface SURFACE_BACK = SURFACES.get(1);
    public static final XmSurface SURFACE_SIDE = SURFACES.get(2);
    
    static final Function<PolyTransform, XmMesh> POLY_FACTORY = transform -> {
        WritableMesh mesh = XmMeshes.claimWritable();
        MutablePolygon writer = mesh.writer();
        writer.colorAll(0, 0xFFFFFFFF);
        writer.lockUV(0, true);
        writer.rotation(0, Rotation.ROTATE_NONE);
        writer.sprite(0, "");
        mesh.saveDefaults();

        writer.surface(SURFACE_BACK);
        writer.setupFaceQuad(Direction.DOWN, 0, 0, 1, 1, 0, Direction.NORTH);
        transform.apply(writer);
        mesh.append();
        
        writer.surface(SURFACE_FRONT);
        writer.setupFaceQuad(Direction.UP, 0, 0, 1, 1, 0, Direction.NORTH);
        transform.apply(writer);
        mesh.append();
        
        writer.surface(SURFACE_SIDE);
        writer.setupFaceQuad(Direction.EAST, 0, 0, 1, 1, 0, Direction.UP);
        transform.apply(writer);
        mesh.append();
        
        writer.surface(SURFACE_SIDE);
        writer.setupFaceQuad(Direction.WEST, 0, 0, 1, 1, 0, Direction.UP);
        transform.apply(writer);
        mesh.append();
        
        writer.surface(SURFACE_FRONT);
        writer.setupFaceQuad(Direction.NORTH, 0, 0, 1, 1, 0, Direction.UP);
        transform.apply(writer);
        mesh.append();
        
        writer.surface(SURFACE_BACK);
        writer.setupFaceQuad(Direction.SOUTH, 0, 0, 1, 1, 0, Direction.UP);
        transform.apply(writer);
        mesh.append();

        return mesh.releaseToReader();
    };
    
    public static final SimplePrimitive INSTANCE = SimplePrimitive.builder()
            .surfaceList(SURFACES)
            .polyFactory(POLY_FACTORY)
            .orientationType(OrientationType.EDGE)
            .build(Xm.idString("cube_edge"));
    
}