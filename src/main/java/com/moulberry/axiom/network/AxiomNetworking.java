package com.moulberry.axiom.network;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.packets.AxiomClientboundPacket;
import com.moulberry.axiom.packets.AxiomServerboundPacket;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge networking bridge for Axiom's protocol. Axiom's packets are native {@link CustomPacketPayload}s,
 * so the wire format is identical to the Fabric build. Registration is {@code optional()} so the client
 * stays compatible with non-NeoForge servers (Paper) that don't do NeoForge channel negotiation.
 */
public final class AxiomNetworking {

    /** Protocol/network version; must match {@link AxiomClientboundPacket#API_VERSION}. */
    public static final String VERSION = String.valueOf(AxiomClientboundPacket.API_VERSION);

    /** Channels used in both directions under one id (see {@link AxiomRawPayload}). */
    private static final Set<ResourceLocation> BIDIRECTIONAL = new HashSet<>();

    private static PayloadRegistrar registrar;

    private AxiomNetworking() {
    }

    /** Subscribed to the mod event bus by {@code AxiomNeoForge}. */
    public static void onRegister(RegisterPayloadHandlersEvent event) {
        registrar = event.registrar(VERSION).optional();
        AxiomPackets.registerAll();
        registrar = null;
    }

    public static <T extends AxiomClientboundPacket> void registerClientbound(
            CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        registrar.playToClient(type, codec, (payload, context) -> ClientEvents.clientboundPackets.add(payload));
    }

    public static <T extends AxiomServerboundPacket> void registerServerbound(
            CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        registrar.playToServer(type, codec,
                (payload, context) -> payload.handle(context.player().getServer(), (ServerPlayer) context.player()));
    }

    /**
     * Registers a channel that carries different messages per direction under one id. NeoForge keys
     * payloads globally by id, so such a channel is registered once, bidirectionally, over raw bytes;
     * the handler decodes into the correct clientbound/serverbound packet from the {@link PacketFlow}.
     */
    public static void registerBidirectional(ResourceLocation id,
            Function<FriendlyByteBuf, ? extends AxiomClientboundPacket> clientDecoder,
            Function<FriendlyByteBuf, ? extends AxiomServerboundPacket> serverDecoder) {
        BIDIRECTIONAL.add(id);
        CustomPacketPayload.Type<AxiomRawPayload> type = new CustomPacketPayload.Type<>(id);
        registrar.playBidirectional(type, AxiomRawPayload.codec(id), (payload, context) -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                ClientEvents.clientboundPackets.add(clientDecoder.apply(buffer));
            } else {
                serverDecoder.apply(buffer).handle(context.player().getServer(), (ServerPlayer) context.player());
            }
        });
    }

    public static void sendToServer(AxiomServerboundPacket packet) {
        PacketDistributor.sendToServer(wrap(packet, packet.id(), packet::write));
    }

    public static void sendToPlayer(ServerPlayer player, AxiomClientboundPacket packet) {
        PacketDistributor.sendToPlayer(player, wrap(packet, packet.packetIdentifier(), packet::write));
    }

    private static CustomPacketPayload wrap(CustomPacketPayload packet, ResourceLocation id, Consumer<FriendlyByteBuf> writer) {
        if (!BIDIRECTIONAL.contains(id)) {
            return packet;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buffer);
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        return new AxiomRawPayload(id, data);
    }
}
