package com.moulberry.axiom.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Raw-bytes payload used for the two Axiom channels that carry <em>different</em> messages in each
 * direction under the same channel name ({@code axiom:annotation_update}, {@code axiom:set_world_property}).
 *
 * <p>Fabric has separate S2C/C2S payload registries, so a channel id may appear in both; NeoForge keys
 * payloads globally by {@link ResourceLocation}, which forbids registering the same id twice. We therefore
 * register these channels once, bidirectionally, carrying the raw payload bytes; {@code AxiomNetworking}
 * decodes them into the correct clientbound/serverbound packet based on the {@code PacketFlow}. The wire
 * format is unchanged (the bytes are exactly what the original per-direction {@code write()} produced).
 */
public record AxiomRawPayload(ResourceLocation channel, byte[] data) implements CustomPacketPayload {

    @Override
    public Type<AxiomRawPayload> type() {
        return new Type<>(this.channel);
    }

    public static StreamCodec<FriendlyByteBuf, AxiomRawPayload> codec(ResourceLocation channel) {
        return CustomPacketPayload.codec(
                (payload, buffer) -> buffer.writeBytes(payload.data),
                buffer -> {
                    byte[] bytes = new byte[buffer.readableBytes()];
                    buffer.readBytes(bytes);
                    return new AxiomRawPayload(channel, bytes);
                });
    }
}
