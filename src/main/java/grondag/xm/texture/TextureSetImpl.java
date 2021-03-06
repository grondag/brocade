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
package grondag.xm.texture;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.function.Consumer;

import org.apiguardian.api.API;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

import grondag.xm.api.modelstate.ModelStateFlags;
import grondag.xm.api.texture.TextureSet;
import grondag.xm.api.texture.TextureSetBuilder;

@API(status = INTERNAL)
public class TextureSetImpl extends AbstractTextureSet implements TextureSet {
	public static TextureSetBuilder builder() {
		return new TextureSetBuilderImpl();
	}

	public static TextureSetBuilder builder(TextureSet template) {
		final TextureSetBuilderImpl result = new TextureSetBuilderImpl();
		result.copyFrom((AbstractTextureSet) template);
		return result;
	}

	public final Identifier id;
	public final int versionMask;
	public final int stateFlags;
	public final String baseTextureName;
	private boolean used = false;

	TextureSetImpl(Identifier id, AbstractTextureSet template) {
		this.id = id;
		baseTextureName = template.rawBaseTextureName;
		copyFrom(template);
		versionMask = Math.max(0, template.versionCount - 1);
		layoutMap = template.layoutMap;

		int flags = template.scale.modelStateFlag | template.layoutMap.layout.modelStateFlag;

		// textures with randomization options also require position information
		if (template.transform.hasRandom) {
			flags |= ModelStateFlags.POSITION;
		}

		if (template.versionCount > 1) {
			flags |= ModelStateFlags.POSITION;
		}

		stateFlags = flags;

		TextureSetRegistryImpl.INSTANCE.add(this);
	}

	@Override
	public Identifier id() {
		return id;
	}

	@Override
	public int index() {
		return TextureSetRegistryImpl.INSTANCE.indexOf(this);
	}

	@Override
	public int stateFlags() {
		return stateFlags;
	}

	@Override
	public void prestitch(Consumer<Identifier> stitcher) {
		layoutMap.prestitch(this, stitcher);
	}

	@Override
	public String sampleTextureName() {
		return layoutMap.sampleTextureName(this);
	}

	private Sprite sampleSprite;

	@Override
	public Sprite sampleSprite() {
		Sprite result = sampleSprite;
		if (result == null) {
			result = TextureSetHelper.blockAtas().getSprite(new Identifier(sampleTextureName()));
			sampleSprite = result;
		}
		return result;
	}

	@Override
	public String textureName(int version) {
		return layoutMap.buildTextureName(this, version & versionMask, 0);
	}

	@Override
	public String textureName(int version, int index) {
		return layoutMap.buildTextureName(this, version & versionMask, index);
	}

	@Override
	public int versionMask() {
		return versionMask;
	}

	@Override
	public String baseTextureName() {
		return baseTextureName;
	}

	@Override
	public void use() {
		used = true;
	}

	@Override
	public boolean used() {
		return used;
	}
}
