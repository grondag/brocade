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

import java.util.function.Consumer;
import java.util.function.Function;

import org.apiguardian.api.API;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;

import grondag.fermion.orientation.api.OrientationType;
import grondag.xm.Xm;
import grondag.xm.api.connect.state.SimpleJoinState;
import grondag.xm.api.mesh.CsgMeshBuilder;
import grondag.xm.api.mesh.MeshHelper;
import grondag.xm.api.mesh.WritableMesh;
import grondag.xm.api.mesh.XmMesh;
import grondag.xm.api.mesh.polygon.MutablePolygon;
import grondag.xm.api.mesh.polygon.PolyTransform;
import grondag.xm.api.modelstate.primitive.PrimitiveState;
import grondag.xm.api.paint.SurfaceTopology;
import grondag.xm.api.primitive.SimplePrimitive;
import grondag.xm.api.primitive.surface.XmSurface;
import grondag.xm.api.primitive.surface.XmSurfaceList;
import grondag.xm.api.texture.TextureOrientation;

@API(status = EXPERIMENTAL)
public class RoundCappedRoundColumn  {
	private RoundCappedRoundColumn() {}

	private static final float INNER_DIAMETER = 0.75f;

	public static final XmSurfaceList SURFACES = XmSurfaceList.builder()
			.add("ends", SurfaceTopology.CUBIC, XmSurface.FLAG_NONE)
			.add("outer", SurfaceTopology.TILED, XmSurface.FLAG_NONE)
			.add("cut", SurfaceTopology.TILED, XmSurface.FLAG_LAMP_GRADIENT)
			.add("inner", SurfaceTopology.TILED, XmSurface.FLAG_LAMP)
			.build();

	public static final XmSurface SURFACE_ENDS = SURFACES.get(0);
	public static final XmSurface SURFACE_OUTER = SURFACES.get(1);
	public static final XmSurface SURFACE_CUT = SURFACES.get(2);
	public static final XmSurface SURFACE_INNER = SURFACES.get(3);

	private static Axis[] AXES = Axis.values();

	static final Function<PrimitiveState, XmMesh> POLY_FACTORY = modelState -> {
		final PolyTransform pt = PolyTransform.get(modelState);

		final CsgMeshBuilder csg = CsgMeshBuilder.threadLocal();
		final SimpleJoinState state = modelState.simpleJoin();

		boolean hasCap = false;

		final Axis axis = AXES[modelState.orientationIndex()];
		if (!state.isJoined(Direction.from(axis, AxisDirection.POSITIVE))) {
			emitOuterSection(csg.input().writer(), pt, 0.25f, axis == Axis.X ? 0 : 0.75f, SURFACE_ENDS, SURFACE_CUT);
			//emitCapSection(csg.input(), pt, axis != Axis.X);
			csg.union();
			hasCap = true;
		}

		if (!state.isJoined(Direction.from(axis, AxisDirection.NEGATIVE))) {
			emitOuterSection(csg.input().writer(), pt, 0.25f, axis == Axis.X ? 0.75f : 0, SURFACE_CUT, SURFACE_ENDS);
			//			emitCapSection(csg.input(), pt, axis == Axis.X);
			csg.union();
			hasCap = true;
		}

		emitCenterSection(csg.input(), pt, hasCap);
		//		emitRoundSection(csg.input(), pt, hasCap);
		csg.union();

		return csg.build();
	};

	private static final void emitCenterSection(WritableMesh mesh, PolyTransform pt, boolean incudeCaps) {
		final MutablePolygon writer = mesh.writer();
		final Consumer<MutablePolygon> transform = p -> {
			p.scaleFromBlockCenter(INNER_DIAMETER, 1, INNER_DIAMETER).apply(pt);
		};

		writer.colorAll(0, 0xFFFFFFFF)
		.surface(SURFACE_INNER)
		.lockUV(0, false)
		.rotation(0, TextureOrientation.IDENTITY)
		.sprite(0, "")
		.saveDefaults();

		final XmSurface surface = incudeCaps ? SURFACE_INNER : null;

		MeshHelper.unitCylinder(mesh.writer(), 16, transform, SURFACE_INNER, surface, surface, 2);
	}

	private static final void emitOuterSection(MutablePolygon writer, PolyTransform pt, float height, float bottom,
			XmSurface topSurface, XmSurface bottomSurface) {

		writer.colorAll(0, 0xFFFFFFFF)
		.surface(SURFACE_CUT)
		.lockUV(0, false)
		.rotation(0, TextureOrientation.IDENTITY)
		.sprite(0, "")
		.saveDefaults();

		MeshHelper.unitCylinder(writer, 16, pt, SURFACE_OUTER, topSurface, bottom == 0, bottomSurface, bottom != 0, 3, bottom, bottom + height);
	}

	public static final SimplePrimitive INSTANCE = SimplePrimitive.builder()
			.surfaceList(SURFACES)
			.polyFactory(POLY_FACTORY)
			.axisJoin(true)
			.orientationType(OrientationType.AXIS)
			.build(Xm.id("round_capped_round_column"));
}
