package com.moulberry.axiom.packets;

import com.moulberry.axiom.network.AxiomNetworking;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface AxiomClientboundPacket extends CustomPacketPayload {
    int API_VERSION = 9;

    ResourceLocation packetIdentifier();

    void write(FriendlyByteBuf var1);

    void handle(Minecraft var1, RegistryAccess var2);

    default void send(ServerPlayer player) {
        AxiomNetworking.sendToPlayer(player, this);
    }

    default Type<? extends CustomPacketPayload> type() {
        return new Type<>(this.packetIdentifier());
    }

    static <T extends AxiomClientboundPacket> void register(ResourceLocation identifier, Function<FriendlyByteBuf, T> decoder) {
        Type<T> type = new Type<>(identifier);
        StreamCodec<RegistryFriendlyByteBuf, T> codec = CustomPacketPayload.codec(AxiomClientboundPacket::write, decoder::apply);
        AxiomNetworking.registerClientbound(type, codec);
    }
}
