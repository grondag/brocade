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

package grondag.xm;

import grondag.xm.collision.CollisionBoxDispatcher;
import grondag.xm.dispatch.XmDispatcher;
import grondag.xm.dispatch.XmVariantProvider;
import grondag.xm.init.XmTexturesImpl;
import grondag.xm.model.ModelPrimitiveRegistryImpl;
import grondag.xm.network.Packets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.render.InvalidateRenderStateCallback;

public class XmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        XmTexturesImpl.init();
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(r -> new XmVariantProvider());
        InvalidateRenderStateCallback.EVENT.register(XmClient::invalidate);
        Packets.initializeClient();
    }

    public static void invalidate() {
        XmDispatcher.INSTANCE.clear();
        CollisionBoxDispatcher.clear();
        ModelPrimitiveRegistryImpl.INSTANCE.invalidateCache();
    }
}