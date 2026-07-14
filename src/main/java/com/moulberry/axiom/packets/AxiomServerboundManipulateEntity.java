package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.EntityDataUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AxiomServerboundManipulateEntity implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:manipulate_entity");
   private final List<AxiomServerboundManipulateEntity.ManipulateEntry> entries;
   private static final Rotation[] ROTATION_VALUES = Rotation.values();
   private static final Component UNSUPPORTED_MESSAGE = Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_manip_entities"))
      .withStyle(ChatFormatting.RED);

   public AxiomServerboundManipulateEntity(List<AxiomServerboundManipulateEntity.ManipulateEntry> entries) {
      this.entries = entries;
   }

   public AxiomServerboundManipulateEntity(FriendlyByteBuf friendlyByteBuf) {
      this.entries = friendlyByteBuf.readList(AxiomServerboundManipulateEntity.ManipulateEntry::read);
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.entries, AxiomServerboundManipulateEntity.ManipulateEntry::write);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.ENTITY_MANIPULATE)) {
         ServerLevel serverLevel = player.serverLevel();

         label135:
         for (AxiomServerboundManipulateEntity.ManipulateEntry entry : this.entries) {
            Entity entity = serverLevel.getEntity(entry.uuid);
            if (entity != null && !(entity instanceof Player) && !entity.hasPassenger(AxiomServerboundManipulateEntity::isPlayer)) {
               if (entry.nbt != null && !entry.nbt.isEmpty()) {
                  CompoundTag compoundTag = EntityDataUtils.saveWithoutId(entity);
                  compoundTag = merge(compoundTag, entry.nbt);
                  EntityDataUtils.load(entity, compoundTag);
               }

               Vec3 entryPos = entry.position();
               if (entryPos != null && entry.relativeMovementSet != null) {
                  double newX = entry.relativeMovementSet.contains(RelativeMovement.X) ? entity.position().x + entryPos.x : entryPos.x;
                  double newY = entry.relativeMovementSet.contains(RelativeMovement.Y) ? entity.position().y + entryPos.y : entryPos.y;
                  double newZ = entry.relativeMovementSet.contains(RelativeMovement.Z) ? entity.position().z + entryPos.z : entryPos.z;
                  float newYaw = entry.relativeMovementSet.contains(RelativeMovement.Y_ROT) ? entity.getYRot() + entry.yaw : entry.yaw;
                  float newPitch = entry.relativeMovementSet.contains(RelativeMovement.X_ROT) ? entity.getXRot() + entry.pitch : entry.pitch;
                  if (entity instanceof HangingEntity hangingEntity) {
                     float changedYaw = newYaw - entity.getYRot();
                     int rotations = Math.round(changedYaw / 90.0F);
                     hangingEntity.rotate(ROTATION_VALUES[rotations & 3]);
                     if (entity instanceof ItemFrame itemFrame && itemFrame.getDirection().getAxis() == Axis.Y) {
                        itemFrame.setRotation(itemFrame.getRotation() - Math.round(changedYaw / 45.0F));
                     }
                  }

                  if (!entity.isPassenger()) {
                     entity.setPos(newX, newY, newZ);
                  }

                  entity.setXRot(newPitch);
                  entity.setYRot(newYaw);
                  entity.setYHeadRot(newYaw);
               }

               if (canManipulatePassengers(entity)) {
                  switch (entry.passengerManipulation) {
                     case NONE:
                     default:
                        continue;
                     case REMOVE_ALL:
                        entity.ejectPassengers();
                        continue;
                     case ADD_LIST:
                        Iterator var23 = entry.passengers.iterator();

                        while (true) {
                           if (!var23.hasNext()) {
                              continue label135;
                           }

                           UUID passengerUuid = (UUID)var23.next();
                           Entity passenger = serverLevel.getEntity(passengerUuid);
                           if (passenger != null
                              && !passenger.isPassenger()
                              && canManipulatePassengers(passenger)
                              && !passenger.hasPassenger(AxiomServerboundManipulateEntity::cannotManipulatePassengers)
                              && !passenger.getSelfAndPassengers().anyMatch(entity2 -> entity2 == entity)) {
                              passenger.startRiding(entity, true);
                           }
                        }
                     case REMOVE_LIST:
                  }

                  for (UUID passengerUuid : entry.passengers) {
                     Entity passenger = serverLevel.getEntity(passengerUuid);
                     if (passenger != null
                        && passenger != entity
                        && canManipulatePassengers(passenger)
                        && !passenger.hasPassenger(AxiomServerboundManipulateEntity::cannotManipulatePassengers)) {
                        Entity vehicle = passenger.getVehicle();
                        if (vehicle == entity) {
                           passenger.stopRiding();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean canManipulatePassengers(Entity entity) {
      return entity != null && entity.getType().canSerialize() && !(entity instanceof Player) && !(entity instanceof Marker);
   }

   private static boolean cannotManipulatePassengers(Entity entity) {
      return !canManipulatePassengers(entity);
   }

   private static boolean isPlayer(Entity entity) {
      return entity instanceof Player;
   }

   private static CompoundTag merge(CompoundTag left, CompoundTag right) {
      if (right.contains("axiom:modify")) {
         right.remove("axiom:modify");
         return right;
      } else {
         for (String key : right.getAllKeys()) {
            Tag tag = right.get(key);
            if (tag instanceof CompoundTag compound) {
               if (compound.isEmpty()) {
                  left.remove(key);
               } else {
                  Optional<CompoundTag> leftCompoundOptional = VersionUtilsNbt.helperCompoundTagGetCompound(left, key);
                  if (leftCompoundOptional.isPresent()) {
                     CompoundTag child = leftCompoundOptional.get();
                     child = merge(child, compound);
                     left.put(key, child);
                  } else {
                     CompoundTag copied = compound.copy();
                     if (copied.contains("axiom:modify")) {
                        copied.remove("axiom:modify");
                     }

                     left.put(key, copied);
                  }
               }
            } else {
               left.put(key, tag.copy());
            }
         }

         return left;
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundManipulateEntity::new);
   }

   @Override
   public void send() {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.MANIPULATE_ENTITY)) {
         ChatUtils.error(UNSUPPORTED_MESSAGE);
      } else {
         AxiomServerboundPacket.super.send();
      }
   }

   public record ManipulateEntry(
      UUID uuid,
      @Nullable Set<RelativeMovement> relativeMovementSet,
      @Nullable Vec3 position,
      float yaw,
      float pitch,
      CompoundTag nbt,
      AxiomServerboundManipulateEntity.PassengerManipulation passengerManipulation,
      List<UUID> passengers
   ) {
      public ManipulateEntry(
         UUID uuid,
         @Nullable Set<RelativeMovement> relativeMovementSet,
         @Nullable Vec3 position,
         float yaw,
         float pitch,
         CompoundTag nbt,
         AxiomServerboundManipulateEntity.PassengerManipulation passengerManipulation,
         List<UUID> passengers
      ) {
         passengers = List.copyOf(passengers);
         this.uuid = uuid;
         this.relativeMovementSet = relativeMovementSet;
         this.position = position;
         this.yaw = yaw;
         this.pitch = pitch;
         this.nbt = nbt;
         this.passengerManipulation = passengerManipulation;
         this.passengers = passengers;
      }

      public ManipulateEntry(UUID uuid, AxiomServerboundManipulateEntity.PassengerManipulation passengerManipulation, List<UUID> passengers) {
         this(uuid, null, null, 0.0F, 0.0F, null, passengerManipulation, passengers);
      }

      public ManipulateEntry(UUID uuid, @Nullable Vec3 position, CompoundTag merge) {
         this(
            uuid,
            position == null ? null : RelativeMovement.ROTATION,
            position,
            0.0F,
            0.0F,
            merge,
            AxiomServerboundManipulateEntity.PassengerManipulation.NONE,
            List.of()
         );
      }

      public ManipulateEntry(UUID uuid, @Nullable Set<RelativeMovement> relativeMovementSet, @Nullable Vec3 position, float yaw, float pitch, CompoundTag merge) {
         this(uuid, relativeMovementSet, position, yaw, pitch, merge, AxiomServerboundManipulateEntity.PassengerManipulation.NONE, List.of());
      }

      public static AxiomServerboundManipulateEntity.ManipulateEntry read(FriendlyByteBuf friendlyByteBuf) {
         UUID uuid = friendlyByteBuf.readUUID();
         int flags = friendlyByteBuf.readByte();
         Set<RelativeMovement> relativeMovementSet = null;
         Vec3 position = null;
         float yaw = 0.0F;
         float pitch = 0.0F;
         if (flags >= 0) {
            relativeMovementSet = RelativeMovement.unpack(flags);
            position = new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble());
            yaw = friendlyByteBuf.readFloat();
            pitch = friendlyByteBuf.readFloat();
         }

         CompoundTag nbt = friendlyByteBuf.readNbt();
         AxiomServerboundManipulateEntity.PassengerManipulation passengerManipulation = (AxiomServerboundManipulateEntity.PassengerManipulation)friendlyByteBuf.readEnum(
            AxiomServerboundManipulateEntity.PassengerManipulation.class
         );
         List<UUID> passengers = List.of();
         if (passengerManipulation == AxiomServerboundManipulateEntity.PassengerManipulation.ADD_LIST
            || passengerManipulation == AxiomServerboundManipulateEntity.PassengerManipulation.REMOVE_LIST) {
            passengers = friendlyByteBuf.readList(buf -> buf.readUUID());
         }

         return new AxiomServerboundManipulateEntity.ManipulateEntry(uuid, relativeMovementSet, position, yaw, pitch, nbt, passengerManipulation, passengers);
      }

      public static void write(FriendlyByteBuf friendlyByteBuf, AxiomServerboundManipulateEntity.ManipulateEntry entry) {
         friendlyByteBuf.writeUUID(entry.uuid);
         if (entry.position != null && entry.relativeMovementSet != null) {
            friendlyByteBuf.writeByte(RelativeMovement.pack(entry.relativeMovementSet));
            friendlyByteBuf.writeDouble(entry.position.x);
            friendlyByteBuf.writeDouble(entry.position.y);
            friendlyByteBuf.writeDouble(entry.position.z);
            friendlyByteBuf.writeFloat(entry.yaw);
            friendlyByteBuf.writeFloat(entry.pitch);
         } else {
            friendlyByteBuf.writeByte(-1);
         }

         friendlyByteBuf.writeNbt(entry.nbt);
         friendlyByteBuf.writeEnum(entry.passengerManipulation);
         if (entry.passengerManipulation == AxiomServerboundManipulateEntity.PassengerManipulation.ADD_LIST
            || entry.passengerManipulation == AxiomServerboundManipulateEntity.PassengerManipulation.REMOVE_LIST) {
            friendlyByteBuf.writeCollection(entry.passengers, (buf, uuid) -> buf.writeUUID(uuid));
         }
      }
   }

   public static enum PassengerManipulation {
      NONE,
      REMOVE_ALL,
      ADD_LIST,
      REMOVE_LIST;
   }
}
