package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.EntityDataUtils;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AxiomServerboundSpawnEntity implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:spawn_entity");
   private final List<AxiomServerboundSpawnEntity.SpawnEntry> entries;
   private static final Rotation[] ROTATION_VALUES = Rotation.values();
   private static final Component UNSUPPORTED_MESSAGE = Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_spawn_entities"))
      .withStyle(ChatFormatting.RED);

   public AxiomServerboundSpawnEntity(List<AxiomServerboundSpawnEntity.SpawnEntry> entries) {
      this.entries = entries;
   }

   public AxiomServerboundSpawnEntity(FriendlyByteBuf friendlyByteBuf) {
      this.entries = friendlyByteBuf.readList(AxiomServerboundSpawnEntity.SpawnEntry::new);
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.entries, AxiomServerboundSpawnEntity.SpawnEntry::write);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.ENTITY_SPAWN)) {
         for (AxiomServerboundSpawnEntity.SpawnEntry entry : this.entries) {
            BlockPos blockPos = BlockPos.containing(entry.position);
            if (Level.isInSpawnableBounds(blockPos)) {
               CompoundTag tag = entry.tag == null ? new CompoundTag() : entry.tag;
               ServerLevel serverLevel = player.serverLevel();
               if (serverLevel.getEntity(entry.newUuid) == null) {
                  if (entry.copyFrom != null) {
                     Entity entityCopyFrom = serverLevel.getEntity(entry.copyFrom);
                     if (entityCopyFrom != null) {
                        CompoundTag compoundTag = EntityDataUtils.saveAsPassenger(entityCopyFrom);
                        if (compoundTag != null) {
                           compoundTag.remove("Dimension");
                           tag = tag.merge(compoundTag);
                        }
                     }
                  }

                  if (tag.contains("id")) {
                     BooleanWrapper usedNewUuid = new BooleanWrapper(false);
                     Entity spawned = VersionUtils.genericLoadEntityRecursive(tag, serverLevel, MobSpawnType.COMMAND, entity -> {
                        if (usedNewUuid.value) {
                           entity.setUUID(UUID.randomUUID());
                        } else {
                           usedNewUuid.value = true;
                           entity.setUUID(entry.newUuid);
                        }

                        if (entity instanceof HangingEntity hangingEntity) {
                           float changedYaw = entry.yaw - entity.getYRot();
                           int rotations = Math.round(changedYaw / 90.0F);
                           hangingEntity.rotate(ROTATION_VALUES[rotations & 3]);
                           if (entity instanceof ItemFrame itemFrame && itemFrame.getDirection().getAxis() == Axis.Y) {
                              itemFrame.setRotation(itemFrame.getRotation() - Math.round(changedYaw / 45.0F));
                           }
                        }

                        entity.setPos(entry.position.x, entry.position.y, entry.position.z);
                        entity.setYRot(entry.yaw);
                        entity.setXRot(entry.pitch);
                        entity.setYHeadRot(entity.getYRot());
                        return entity;
                     });
                     if (spawned != null) {
                        serverLevel.tryAddFreshEntityWithPassengers(spawned);
                     }
                  }
               }
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSpawnEntity::new);
   }

   @Override
   public void send() {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.CREATE_ENTITY)) {
         ChatUtils.error(UNSUPPORTED_MESSAGE);
      } else {
         AxiomServerboundPacket.super.send();
      }
   }

   public record SpawnEntry(UUID newUuid, Vec3 position, float yaw, float pitch, @Nullable UUID copyFrom, CompoundTag tag) {
      public SpawnEntry(FriendlyByteBuf friendlyByteBuf) {
         this(
            friendlyByteBuf.readUUID(),
            new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble()),
            friendlyByteBuf.readFloat(),
            friendlyByteBuf.readFloat(),
            (UUID)friendlyByteBuf.readNullable(buf -> buf.readUUID()),
            friendlyByteBuf.readNbt()
         );
      }

      public static void write(FriendlyByteBuf friendlyByteBuf, AxiomServerboundSpawnEntity.SpawnEntry entry) {
         friendlyByteBuf.writeUUID(entry.newUuid);
         friendlyByteBuf.writeDouble(entry.position.x);
         friendlyByteBuf.writeDouble(entry.position.y);
         friendlyByteBuf.writeDouble(entry.position.z);
         friendlyByteBuf.writeFloat(entry.yaw);
         friendlyByteBuf.writeFloat(entry.pitch);
         friendlyByteBuf.writeNullable(entry.copyFrom, (buf, uuid) -> buf.writeUUID(uuid));
         friendlyByteBuf.writeNbt(entry.tag);
      }
   }
}
