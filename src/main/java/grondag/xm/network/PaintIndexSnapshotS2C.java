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
package grondag.xm.network;

import static org.apiguardian.api.API.Status.INTERNAL;

import io.netty.buffer.Unpooled;
import org.apiguardian.api.API;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import grondag.xm.Xm;
import grondag.xm.api.paint.XmPaint;
import grondag.xm.paint.PaintIndexImpl;

@API(status = INTERNAL)
public abstract class PaintIndexSnapshotS2C {
	private PaintIndexSnapshotS2C() {
	}

	public static final Identifier ID = new Identifier(Xm.MODID, "pxs");

	public static Packet<?> toPacket(PaintIndexImpl paintIndex) {
		final PacketByteBuf pBuff = new PacketByteBuf(Unpooled.buffer());
		paintIndex.toBytes(pBuff);
		return ServerPlayNetworking.createS2CPacket(ID, pBuff);
	}

	public static void accept(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf pBuff, PacketSender responseSender) {
		final XmPaint[] paints = PaintIndexImpl.arrayFromBytes(pBuff);

		if (client.isOnThread()) {
			PaintIndexImpl.CLIENT.fromArray(paints);
		} else {
			client.execute(() -> PaintIndexImpl.CLIENT.fromArray(paints));
		}
	}
}
