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

package grondag.xm.api.mesh;

import java.util.List;

import org.joml.Vector4f;

import com.google.common.collect.ImmutableList;

import grondag.xm.mesh.vertex.Vec3Function;
import grondag.xm.mesh.vertex.Vec3f;
import grondag.xm.mesh.vertex.Vertex3f;
import grondag.xm.mesh.vertex.VertexCollection;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class QuadHelper {
    public static final float EPSILON = 1.0E-5F;

    public static final List<BakedQuad> EMPTY_QUAD_LIST = new ImmutableList.Builder<BakedQuad>().build();

    @Deprecated
    public static boolean epsilonEquals(double first, double second) {
        return Math.abs(first - second) < EPSILON;
    }

    public static boolean epsilonEquals(float first, float second) {
        return Math.abs(first - second) < EPSILON;
    }

    private static final Direction[] FACES = Direction.values();
    
    public static Direction faceForNormal(final float x, final float y, final float z) {
        Direction result = null;

        double minDiff = 0.0F;

        for (int i = 0; i < 6; i++) {
            final Direction f = FACES[i];
            Vec3i faceNormal = f.getVector();
            float diff = Vec3Function.dotProduct(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ(), x, y, z);

            if (diff >= 0.0 && diff > minDiff) {
                minDiff = diff;
                result = f;
            }
        }

        if (result == null) {
            return Direction.UP;
        } else {
            return result;
        }
    }

    public static Direction faceForNormal(Vec3f normal) {
        return faceForNormal(normal.x(), normal.y(), normal.z());
    }

    public static Direction faceForNormal(Vector4f normal) {
        return faceForNormal(normal.x, normal.y, normal.z);
    }

    /** returns the face that is normally the "top" of the given face */
    public static Direction defaultTopOf(Direction faceIn) {
        switch (faceIn) {
        case UP:
            return Direction.NORTH;
        case DOWN:
            return Direction.SOUTH;
        default:
            return Direction.UP;
        }
    }

    public static Direction bottomOf(Direction faceIn, Direction topFace) {
        return topFace.getOpposite();
    }

    public static Direction positiveDirection(Direction.Axis axis) {
        switch (axis) {
        case Y:
            return Direction.UP;
        case X:
            return Direction.EAST;
        default:
            return Direction.NORTH;
        }
    }

    public static Direction leftOf(Direction faceIn, Direction topFace) {
        return QuadHelper.rightOf(faceIn, topFace).getOpposite();
    }

    public static Direction rightOf(Direction faceIn, Direction topFace) {
        switch (faceIn) {
        case NORTH:
            switch (topFace) {
            case UP:
                return Direction.WEST;
            case EAST:
                return Direction.UP;
            case DOWN:
                return Direction.EAST;
            case WEST:
            default:
                return Direction.DOWN;
            }
        case SOUTH:
            switch (topFace) {
            case UP:
                return Direction.EAST;
            case EAST:
                return Direction.DOWN;
            case DOWN:
                return Direction.WEST;
            case WEST:
            default:
                return Direction.UP;
            }
        case EAST:
            switch (topFace) {
            case UP:
                return Direction.NORTH;
            case NORTH:
                return Direction.DOWN;
            case DOWN:
                return Direction.SOUTH;
            case SOUTH:
            default:
                return Direction.UP;
            }
        case WEST:
            switch (topFace) {
            case UP:
                return Direction.SOUTH;
            case NORTH:
                return Direction.UP;
            case DOWN:
                return Direction.NORTH;
            case SOUTH:
            default:
                return Direction.DOWN;
            }
        case UP:
            switch (topFace) {
            case NORTH:
                return Direction.EAST;
            case EAST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.WEST;
            case WEST:
            default:
                return Direction.NORTH;
            }
        case DOWN:
        default:
            switch (topFace) {
            case NORTH:
                return Direction.WEST;
            case EAST:
                return Direction.NORTH;
            case SOUTH:
                return Direction.EAST;
            case WEST:
            default:
                return Direction.SOUTH;
            }
        }
    }

    public static boolean isConvex(VertexCollection vertices) {
        final int vertexCount = vertices.vertexCount();
        if (vertexCount == 3)
            return true;

        float testX = 0;
        float testY = 0;
        float testZ = 0;
        boolean needTest = true;

        Vertex3f priorVertex = vertices.getPos(vertexCount - 2);
        Vertex3f thisVertex = vertices.getPos(vertexCount - 1);

        for (int nextIndex = 0; nextIndex < vertexCount; nextIndex++) {
            Vertex3f nextVertex = vertices.getPos(nextIndex);

            final float ax = thisVertex.x() - priorVertex.x();
            final float ay = thisVertex.y() - priorVertex.y();
            final float az = thisVertex.z() - priorVertex.z();

            final float bx = nextVertex.x() - thisVertex.x();
            final float by = nextVertex.y() - thisVertex.y();
            final float bz = nextVertex.z() - thisVertex.z();

            final float crossX = ay * bz - az * by;
            final float crossY = az * bx - ax * bz;
            final float crossZ = ax * by - ay * bx;

            if (needTest) {
                needTest = false;
                testX = crossX;
                testY = crossY;
                testZ = crossZ;
            } else if (testX * crossX + testY * crossY + testZ * crossZ < 0) {
                return false;
            }

            priorVertex = thisVertex;
            thisVertex = nextVertex;
        }
        return true;
    }
}