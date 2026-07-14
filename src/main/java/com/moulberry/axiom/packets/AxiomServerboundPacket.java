package com.moulberry.axiom.packets;

import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.network.AxiomNetworking;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public interface AxiomServerboundPacket extends CustomPacketPayload {
    int API_VERSION = 9;

    ResourceLocation id();

    void write(FriendlyByteBuf var1);

    void handle(MinecraftServer var1, ServerPlayer var2);

    default void send() {
        AxiomNetworking.sendToServer(this);
    }

    static boolean canUseAxiom(ServerPlayer player, AxiomPermission axiomPermission) {
        return AxiomServer.canUseAxiom(player, axiomPermission);
    }

    default Type<? extends CustomPacketPayload> type() {
        return new Type<>(this.id());
    }

    static <T extends AxiomServerboundPacket> void register(ResourceLocation identifier, Function<FriendlyByteBuf, T> decoder) {
        Type<T> type = new Type<>(identifier);
        StreamCodec<RegistryFriendlyByteBuf, T> codec = CustomPacketPayload.codec(AxiomServerboundPacket::write, decoder::apply);
        AxiomNetworking.registerServerbound(type, codec);
    }
}
