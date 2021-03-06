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

import grondag.fermion.orientation.api.OrientationType;
import grondag.xm.Xm;
import grondag.xm.api.primitive.SimplePrimitive;
import grondag.xm.api.primitive.surface.XmSurface;
import grondag.xm.api.primitive.surface.XmSurfaceList;

public class CubeWithHorizontalFace  {
	public static final XmSurfaceList SURFACES = CubeWithFace.SURFACES;

	public static final XmSurface SURFACE_FRONT = SURFACES.get(0);
	public static final XmSurface SURFACE_SIDES = SURFACES.get(1);

	public static final SimplePrimitive INSTANCE = SimplePrimitive.builder()
			.surfaceList(SURFACES)
			.polyFactory(CubeWithFace.POLY_FACTORY)
			.orientationType(OrientationType.HORIZONTAL_FACE)
			.build(Xm.id("cube_horizontal"));
}
