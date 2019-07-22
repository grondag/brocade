package grondag.xm2.api.model;

import javax.annotation.Nullable;

import grondag.xm2.model.state.ModelStatesImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;

public interface ModelStates {

    public static @Nullable OwnedModelState fromTag(CompoundTag tag) {
        return ModelStatesImpl.fromTag(tag);
    }
    
    public static @Nullable OwnedModelState fromBuffer(PacketByteBuf buf) {
        return ModelStatesImpl.fromBuffer(buf);
    }
}