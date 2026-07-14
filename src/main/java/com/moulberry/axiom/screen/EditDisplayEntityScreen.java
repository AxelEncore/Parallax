package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.math.Transformation;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.displayentity.DisplayEntityHelper;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.displayentity.DisplayEntityObject;
import com.moulberry.axiom.displayentity.GizmoMode;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton.Builder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Display.RenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class EditDisplayEntityScreen extends Screen {
   private static final ResourceLocation BACKGROUND = ResourceLocation.parse("axiom:background");
   private int leftPos;
   private int topPos;
   private int menuWidth;
   private int menuHeight;
   private UUID editingEntityUuid;
   private boolean showGroupWithNearby = false;
   private float groupNearbyRange = 1.0F;
   private boolean editTransformationScreen = false;
   private Vector3f translation;
   private Quaternionf left;
   private Vector3f scale;
   private Quaternionf right;

   public EditDisplayEntityScreen(UUID editingEntityUuid) {
      super(Component.translatable("axiom.edit_display_entity.title"));
      this.editingEntityUuid = editingEntityUuid;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      this.clearWidgets();
      if (this.editTransformationScreen) {
         this.initEditTransformation();
      } else {
         this.initMain();
      }
   }

   private void initMain() {
      this.menuWidth = 320;
      this.menuHeight = 195;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int y = this.topPos + 17;
      int x = this.leftPos + 8;
      int itemWidth = 150;
      this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.edit_properties"), button -> {
         DisplayEntityObject input = DisplayEntityHelper.getObjectFromEntity(DisplayEntityManipulator.getActiveDisplayEntity());
         Minecraft.getInstance().setScreen(new CreateDisplayEntityScreen(input, object -> {
            if (!this.closeSanityCheck()) {
               DisplayEntityHelper.applyDataTo(DisplayEntityManipulator.getActiveDisplayEntity(), object);
            }
         }, true));
      }).bounds(x, y, itemWidth, 20).build());
      y += 24;
      this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.edit_transformation"), button -> {
         this.editTransformationScreen = true;
         this.init();
      }).bounds(x, y, itemWidth, 20).build());
      y += 24;
      Display currentDisplay = DisplayEntityManipulator.getActiveDisplayEntity();
      Vector3fc currentTranslation = (Vector3fc)currentDisplay.getEntityData().get(Display.DATA_TRANSLATION_ID);
      Quaternionfc currentLeft = (Quaternionfc)currentDisplay.getEntityData().get(Display.DATA_LEFT_ROTATION_ID);
      Vector3fc currentScale = (Vector3fc)currentDisplay.getEntityData().get(Display.DATA_SCALE_ID);
      Quaternionfc currentRight = (Quaternionfc)currentDisplay.getEntityData().get(Display.DATA_RIGHT_ROTATION_ID);
      AbstractWidget resetTranslation = (AbstractWidget)this.addRenderableWidget(
         Button.builder(Component.translatable("axiom.edit_display_entity.reset_translation"), button -> {
            if (!this.closeSanityCheck()) {
               Display display = DisplayEntityManipulator.getActiveDisplayEntity();
               Quaternionfc left = (Quaternionfc)display.getEntityData().get(Display.DATA_LEFT_ROTATION_ID);
               Vector3fc scale = (Vector3fc)display.getEntityData().get(Display.DATA_SCALE_ID);
               Quaternionfc right = (Quaternionfc)display.getEntityData().get(Display.DATA_RIGHT_ROTATION_ID);
               Matrix4f matrix = new Matrix4f();
               matrix.rotate(left);
               matrix.scale(scale);
               matrix.rotate(right);
               DisplayEntityHelper.applyTransformation(display, matrix);
               this.init();
            }
         }).bounds(x, y, itemWidth, 20).build()
      );
      if (currentTranslation.x() == 0.0F && currentTranslation.y() == 0.0F && currentTranslation.z() == 0.0F) {
         resetTranslation.active = false;
      }

      y += 24;
      AbstractWidget resetRotation = (AbstractWidget)this.addRenderableWidget(
         Button.builder(Component.translatable("axiom.edit_display_entity.reset_rotation"), button -> {
            if (!this.closeSanityCheck()) {
               Display display = DisplayEntityManipulator.getActiveDisplayEntity();
               Vector3fc translation = (Vector3fc)display.getEntityData().get(Display.DATA_TRANSLATION_ID);
               Vector3fc scale = (Vector3fc)display.getEntityData().get(Display.DATA_SCALE_ID);
               Matrix4f matrix = new Matrix4f();
               matrix.translate(translation);
               matrix.scale(scale);
               DisplayEntityHelper.applyTransformation(display, matrix);
               this.init();
            }
         }).bounds(x, y, itemWidth, 20).build()
      );
      if (BrushShape.isQuaternionIdentity(currentLeft) && BrushShape.isQuaternionIdentity(currentRight)) {
         resetRotation.active = false;
      }

      y += 24;
      AbstractWidget resetScale = (AbstractWidget)this.addRenderableWidget(
         Button.builder(Component.translatable("axiom.edit_display_entity.reset_scale"), button -> {
            if (!this.closeSanityCheck()) {
               Display display = DisplayEntityManipulator.getActiveDisplayEntity();
               Vector3fc translation = (Vector3fc)display.getEntityData().get(Display.DATA_TRANSLATION_ID);
               Quaternionfc left = (Quaternionfc)display.getEntityData().get(Display.DATA_LEFT_ROTATION_ID);
               Quaternionfc right = (Quaternionfc)display.getEntityData().get(Display.DATA_RIGHT_ROTATION_ID);
               Matrix4f matrix = new Matrix4f();
               matrix.translate(translation);
               matrix.rotate(left);
               matrix.rotate(right);
               DisplayEntityHelper.applyTransformation(display, matrix);
               this.init();
            }
         }).bounds(x, y, itemWidth, 20).build()
      );
      if (Math.abs(currentScale.x() - 1.0F) < 1.0E-5 && Math.abs(currentScale.y() - 1.0F) < 1.0E-5 && Math.abs(currentScale.z() - 1.0F) < 1.0E-5) {
         resetScale.active = false;
      }

      y += 24;
      Builder<GizmoMode> gizmoModeCycleButton = CycleButton.<GizmoMode>builder(CreateDisplayEntityScreen::toComponent)
         .withInitialValue(DisplayEntityManipulator.gizmoMode);
      CycleButton<?> gizmoModeButton = gizmoModeCycleButton.withValues(GizmoMode.values())
         .create(
            x, y, itemWidth, 20, Component.translatable("axiom.edit_display_entity.gizmo_mode"), (button, value) -> DisplayEntityManipulator.gizmoMode = value
         );
      this.addRenderableWidget(gizmoModeButton);
      x = this.leftPos + 8 + 160 - 6;
      y = this.topPos + 17;
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("axiom.edit_display_entity.duplicate"),
               button -> {
                  if (!this.closeSanityCheck()) {
                     Display display = DisplayEntityManipulator.getActiveDisplayEntity();
                     new AxiomServerboundSpawnEntity(
                           List.of(
                              new AxiomServerboundSpawnEntity.SpawnEntry(
                                 UUID.randomUUID(), display.position(), display.getYRot(), display.getXRot(), display.getUUID(), null
                              )
                           )
                        )
                        .send();
                     Minecraft.getInstance().setScreen(null);
                  }
               }
            )
            .bounds(x, y, itemWidth, 20)
            .build()
      );
      y += 24;
      this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.grouping"), button -> {
         this.showGroupWithNearby = !this.showGroupWithNearby;
         this.init();
      }).bounds(x, y, itemWidth, 20).build());
      if (this.showGroupWithNearby) {
         int var23 = y + 20;
         Component initialTitle = Component.translatable(
            "axiom.edit_display_entity.range", new Object[]{Component.literal(String.format("%.2f", this.groupNearbyRange))}
         );
         this.addRenderableWidget(
            new AbstractSliderButton(x, var23, itemWidth, 20, initialTitle, this.groupNearbyRange / 16.0F) {
               protected void updateMessage() {
                  Component title = Component.translatable(
                     "axiom.edit_display_entity.range", new Object[]{Component.literal(String.format("%.2f", EditDisplayEntityScreen.this.groupNearbyRange))}
                  );
                  this.setMessage(title);
               }

               protected void applyValue() {
                  EditDisplayEntityScreen.this.groupNearbyRange = (float)this.value * 16.0F;
               }
            }
         );
         int var24 = var23 + 20;
         this.addRenderableWidget(
            Button.builder(
                  Component.translatable("axiom.edit_display_entity.group_with_nearby_in_range"),
                  button -> {
                     if (!this.closeSanityCheck()) {
                        Display display = DisplayEntityManipulator.getActiveDisplayEntity();
                        if (!display.isPassenger()) {
                           List<AxiomServerboundManipulateEntity.ManipulateEntry> entries = new ArrayList<>();
                           Vec3 displayPos = display.position();
                           float range = this.groupNearbyRange + 0.01F;
                           AABB aabb = new AABB(
                              displayPos.x - range,
                              displayPos.y - range,
                              displayPos.z - range,
                              displayPos.x + range,
                              displayPos.y + range,
                              displayPos.z + range
                           );
                           List<UUID> passengers = new ArrayList<>();
                           removeYawPitchDisplay(display, entries);

                           for (Display nearby : display.level().getEntitiesOfClass(Display.class, aabb)) {
                              if (!nearby.isPassenger() && nearby != display) {
                                 Vec3 deltaToMain = nearby.position().subtract(displayPos);
                                 offsetDisplayRecursive(nearby, deltaToMain, display.position(), entries);
                                 passengers.add(nearby.getUUID());
                              }
                           }

                           entries.add(
                              new AxiomServerboundManipulateEntity.ManipulateEntry(
                                 display.getUUID(), AxiomServerboundManipulateEntity.PassengerManipulation.ADD_LIST, passengers
                              )
                           );
                           new AxiomServerboundManipulateEntity(entries).send();
                           DisplayEntityManipulator.disableActive();
                           Minecraft.getInstance().setScreen(null);
                        }
                     }
                  }
               )
               .bounds(x, var24, itemWidth, 20)
               .build()
         );
         y = var24 + 20;
         Button ungroupChildren = Button.builder(
               Component.translatable("axiom.edit_display_entity.ungroup_children"),
               button -> {
                  if (!this.closeSanityCheck()) {
                     Display display = DisplayEntityManipulator.getActiveDisplayEntity();
                     List<AxiomServerboundManipulateEntity.ManipulateEntry> entries = new ArrayList<>();
                     removeYawPitchDisplay(display, entries);
                     entries.add(
                        new AxiomServerboundManipulateEntity.ManipulateEntry(
                           display.getUUID(), AxiomServerboundManipulateEntity.PassengerManipulation.REMOVE_ALL, List.of()
                        )
                     );

                     for (Entity passenger : display.getPassengers()) {
                        if (passenger instanceof Display displayPassenger) {
                           Matrix4f matrix4f = DisplayEntityManipulator.getTransformationMatrix(displayPassenger);
                           if (passenger instanceof BlockDisplay) {
                              matrix4f.translate(0.5F, 0.5F, 0.5F);
                           }

                           Quaternionf orientation = calculateStaticOrientation(displayPassenger.renderState(), displayPassenger, new Quaternionf());
                           matrix4f.rotateLocal(orientation);
                           Vector3f translation = matrix4f.getTranslation(new Vector3f());
                           Vec3 position = display.position().add(translation.x, translation.y, translation.z);
                           offsetDisplayRecursive(displayPassenger, new Vec3(translation.negate()), position, entries);
                        }
                     }

                     new AxiomServerboundManipulateEntity(entries).send();
                     DisplayEntityManipulator.disableActive();
                     Minecraft.getInstance().setScreen(null);
                  }
               }
            )
            .bounds(x, y, itemWidth, 20)
            .build();
         if (DisplayEntityManipulator.getActiveDisplayEntity().getPassengers().isEmpty()) {
            ungroupChildren.active = false;
         }

         this.addRenderableWidget(ungroupChildren);
      }

      y += 24;
      this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.remove"), button -> {
         if (!this.closeSanityCheck()) {
            DisplayEntityHelper.killRecursive(DisplayEntityManipulator.getActiveDisplayEntity());
            Minecraft.getInstance().setScreen(null);
         }
      }).bounds(x, y, itemWidth, 20).build());
      y += 24;
      if (!this.showGroupWithNearby) {
         this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.copy_summon_command"), button -> {
            if (!this.closeSanityCheck()) {
               DisplayEntityManipulator.tryCopyToClipboard();
               Minecraft.getInstance().setScreen(null);
            }
         }).bounds(x, y, itemWidth, 20).build());
         y += 24;
         this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.copy_coordinates"), button -> {
            if (!this.closeSanityCheck()) {
               Display display = DisplayEntityManipulator.getActiveDisplayEntity();
               String coordinates = display.position().x + " " + display.position().y + " " + display.position().z;
               Minecraft.getInstance().keyboardHandler.setClipboard(coordinates);
               Minecraft.getInstance().setScreen(null);
            }
         }).bounds(x, y, itemWidth, 20).build());
         y += 24;
         this.addRenderableWidget(Button.builder(Component.translatable("axiom.edit_display_entity.copy_transform_command"), button -> {
            if (!this.closeSanityCheck()) {
               DisplayEntityManipulator.tryCopyInterpolateToClipboard();
               Minecraft.getInstance().setScreen(null);
            }
         }).bounds(x, y, itemWidth, 20).build());
      }

      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> Minecraft.getInstance().setScreen(null))
            .bounds(this.leftPos + 8, this.topPos + this.menuHeight - 20 - 8, this.menuWidth - 16, 20)
            .build()
      );
   }

   public static Quaternionf calculateStaticOrientation(RenderState renderState, Display display, Quaternionf quaternionf) {
      return renderState == null ? quaternionf : calculateStaticOrientation(renderState.billboardConstraints(), display, quaternionf);
   }

   private static Quaternionf calculateStaticOrientation(BillboardConstraints billboardConstraints, Display display, Quaternionf quaternionf) {
      if (billboardConstraints == null) {
         return quaternionf;
      } else {
         return switch (billboardConstraints) {
            case FIXED -> quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * display.getYRot(), (float) (Math.PI / 180.0) * display.getXRot(), 0.0F);
            case HORIZONTAL -> quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * display.getYRot(), 0.0F, 0.0F);
            case VERTICAL -> quaternionf.rotationYXZ(0.0F, (float) (Math.PI / 180.0) * display.getXRot(), 0.0F);
            case CENTER -> quaternionf.rotationYXZ(0.0F, 0.0F, 0.0F);
            default -> throw new IncompatibleClassChangeError();
         };
      }
   }

   private static void removeYawPitchDisplay(Display display, List<AxiomServerboundManipulateEntity.ManipulateEntry> entries) {
      Matrix4f matrix4f = DisplayEntityManipulator.getTransformationMatrix(display);
      Quaternionf orientation = calculateStaticOrientation(display.renderState(), display, new Quaternionf());
      matrix4f.rotateLocal(orientation);
      Transformation transformation = VersionUtils.helperTransformationNew(matrix4f);
      transformation.getLeftRotation();
      Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(tag -> {
         CompoundTag root = new CompoundTag();
         root.put("transformation", tag);
         entries.add(new AxiomServerboundManipulateEntity.ManipulateEntry(display.getUUID(), Set.of(), display.position(), 0.0F, 0.0F, root));
      });

      for (Entity passenger : display.getPassengers()) {
         if (passenger instanceof Display displayPassenger) {
            removeYawPitchDisplay(displayPassenger, entries);
         }
      }
   }

   private static void offsetDisplayRecursive(
      Display display, Vec3 delta, @Nullable Vec3 newPosition, List<AxiomServerboundManipulateEntity.ManipulateEntry> entries
   ) {
      Matrix4f matrix4f = DisplayEntityManipulator.getTransformationMatrix(display);
      Quaternionf orientation = calculateStaticOrientation(display.renderState(), display, new Quaternionf());
      matrix4f.rotateLocal(orientation);
      matrix4f.translateLocal((float)delta.x, (float)delta.y, (float)delta.z);
      Transformation transformation = VersionUtils.helperTransformationNew(matrix4f);
      transformation.getLeftRotation();
      Transformation.CODEC
         .encodeStart(NbtOps.INSTANCE, transformation)
         .result()
         .ifPresent(
            tag -> {
               CompoundTag root = new CompoundTag();
               root.put("transformation", tag);
               entries.add(
                  new AxiomServerboundManipulateEntity.ManipulateEntry(
                     display.getUUID(), Set.of(), newPosition != null ? newPosition : display.position(), 0.0F, 0.0F, root
                  )
               );
            }
         );

      for (Entity passenger : display.getPassengers()) {
         if (passenger instanceof Display displayPassenger) {
            offsetDisplayRecursive(displayPassenger, delta, null, entries);
         }
      }
   }

   private void initEditTransformation() {
      this.menuWidth = 195;
      this.menuHeight = 176;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int halfWidth = (this.menuWidth - 16) / 2 - 1;
      Display originalDisplay = DisplayEntityManipulator.getActiveDisplayEntity();
      this.translation = new Vector3f((Vector3fc)originalDisplay.getEntityData().get(Display.DATA_TRANSLATION_ID));
      this.left = new Quaternionf((Quaternionfc)originalDisplay.getEntityData().get(Display.DATA_LEFT_ROTATION_ID));
      this.scale = new Vector3f((Vector3fc)originalDisplay.getEntityData().get(Display.DATA_SCALE_ID));
      this.right = new Quaternionf((Quaternionfc)originalDisplay.getEntityData().get(Display.DATA_RIGHT_ROTATION_ID));
      int x = this.leftPos + 8;
      int y = this.topPos + 6;
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, Component.literal(AxiomI18n.get("axiom.hardcoded.translation_offset")).withStyle(Style.EMPTY.withColor(4210752)), this.font
         )
      );
      y += 10;
      this.createEditBox(this.translation.x, f -> this.translation.x = f, x, y, 0, 3);
      this.createEditBox(this.translation.y, f -> this.translation.y = f, x, y, 1, 3);
      this.createEditBox(this.translation.z, f -> this.translation.z = f, x, y, 2, 3);
      y += 24;
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, Component.literal(AxiomI18n.get("axiom.hardcoded.left_rotation_quat")).withStyle(Style.EMPTY.withColor(4210752)), this.font
         )
      );
      y += 10;
      this.createEditBox(this.left.x, f -> this.left.x = f, x, y, 0, 4);
      this.createEditBox(this.left.y, f -> this.left.y = f, x, y, 1, 4);
      this.createEditBox(this.left.z, f -> this.left.z = f, x, y, 2, 4);
      this.createEditBox(this.left.w, f -> this.left.w = f, x, y, 3, 4);
      y += 24;
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(x + 1, y + 1, 0, 0, Component.literal(AxiomI18n.get("axiom.hardcoded.scale")).withStyle(Style.EMPTY.withColor(4210752)), this.font)
      );
      y += 10;
      this.createEditBox(this.scale.x, f -> this.scale.x = f, x, y, 0, 3);
      this.createEditBox(this.scale.y, f -> this.scale.y = f, x, y, 1, 3);
      this.createEditBox(this.scale.z, f -> this.scale.z = f, x, y, 2, 3);
      y += 24;
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, Component.literal(AxiomI18n.get("axiom.hardcoded.right_rotation_quat")).withStyle(Style.EMPTY.withColor(4210752)), this.font
         )
      );
      y += 10;
      this.createEditBox(this.right.x, f -> this.right.x = f, x, y, 0, 4);
      this.createEditBox(this.right.y, f -> this.right.y = f, x, y, 1, 4);
      this.createEditBox(this.right.z, f -> this.right.z = f, x, y, 2, 4);
      this.createEditBox(this.right.w, f -> this.right.w = f, x, y, 3, 4);
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> Minecraft.getInstance().setScreen(null))
            .bounds(this.leftPos + 8, this.topPos + this.menuHeight - 20 - 8, halfWidth, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(this.leftPos + 8 + halfWidth + 1, this.topPos + this.menuHeight - 20 - 8, halfWidth, 20)
            .build()
      );
   }

   public void onClose() {
      super.onClose();
      if (!this.closeSanityCheck()) {
         if (this.editTransformationScreen) {
            Display display = DisplayEntityManipulator.getActiveDisplayEntity();
            Matrix4f matrix = new Matrix4f();
            matrix.translate(this.translation);
            matrix.rotate(this.left);
            matrix.scale(this.scale);
            matrix.rotate(this.right);
            DisplayEntityHelper.applyTransformation(display, matrix);
            DisplayEntityManipulator.disableActive();
         }
      }
   }

   private void createEditBox(float initialValue, Consumer<Float> setter, int x, int y, int index, int count) {
      int width = 179 / count;
      EditBox editBox = new EditBox(this.font, x + (width + 1) * index, y, width, 20, CommonComponents.EMPTY);
      if (Math.round(initialValue) * 1000 == Math.round(initialValue * 1000.0F)) {
         editBox.setValue(String.valueOf(Math.round(initialValue)));
      } else {
         editBox.setValue(String.format(Locale.ROOT, "%.3f", initialValue));
      }

      editBox.setResponder(string -> {
         try {
            setter.accept(Float.parseFloat(string));
            editBox.setTextColor(-1);
         } catch (Exception var4) {
            editBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(editBox);
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, this.menuWidth, this.menuHeight);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      if (!this.closeSanityCheck()) {
         if (!this.editTransformationScreen) {
            int y = this.topPos + 6;
            guiGraphics.drawString(this.font, this.title, this.leftPos + 8, y, -12566464, false);
         }
      }
   }

   private boolean closeSanityCheck() {
      Display display = DisplayEntityManipulator.getActiveDisplayEntity();
      if (display != null && !display.isRemoved() && display.getUUID() == this.editingEntityUuid) {
         return false;
      } else {
         Minecraft.getInstance().setScreen(null);
         return true;
      }
   }
}
