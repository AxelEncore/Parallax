package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.marker.MarkerData;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.NbtHelper;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EditMarkerScreen extends Screen {
   private static final ResourceLocation BACKGROUND = ResourceLocation.parse("axiom:background");
   private int leftPos;
   private int topPos;
   private int menuWidth;
   private int menuHeight;
   private final UUID editingEntityUuid;
   private CompoundTag data;
   private CompoundTag hiddenData = new CompoundTag();
   private EditBox nameEditBox = null;
   private EditBox[] regionEditBox = new EditBox[6];
   private MultiLineEditBox dataEditBox = null;
   private CommandSyntaxException syntaxException = null;

   public EditMarkerScreen(UUID editingEntityUuid, CompoundTag data) {
      super(Component.literal(AxiomI18n.get("axiom.hardcoded.marker_entity_paren") + editingEntityUuid + ")"));
      this.editingEntityUuid = editingEntityUuid;
      this.data = data;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      this.clearWidgets();
      this.initMain();
   }

   private void initMain() {
      this.menuWidth = 320;
      this.menuHeight = 240;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int x = this.leftPos + 8;
      int y = this.topPos + 17;
      int itemWidth = 143;
      BooleanWrapper updatingTextBoxes = new BooleanWrapper(false);
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(x + 1, y + 1, 0, 0, Component.literal(AxiomI18n.get("axiom.hardcoded.name")).withStyle(Style.EMPTY.withColor(4210752)), this.font)
      );
      y += 10;
      this.nameEditBox = new EditBox(this.font, x, y, itemWidth, 20, Component.literal(AxiomI18n.get("axiom.hardcoded.name")));
      this.nameEditBox.setValue(VersionUtilsNbt.helperCompoundTagGetStringOr(this.data, "name", ""));
      this.nameEditBox.setResponder(string -> {
         if (!this.closeSanityCheck()) {
            if (!updatingTextBoxes.value) {
               updatingTextBoxes.value = true;
               if (string.isEmpty()) {
                  if (this.data.contains("name")) {
                     this.data.remove("name");
                  }
               } else {
                  this.data.putString("name", string);
               }

               this.updateNbtField();
               updatingTextBoxes.value = false;
            }
         }
      });
      this.addRenderableWidget(this.nameEditBox);
      y += 22;
      this.addRenderableWidget(Button.builder(Component.literal(AxiomI18n.get("axiom.hardcoded.set_yaw")), button -> {
         if (!this.closeSanityCheck()) {
            if (!updatingTextBoxes.value) {
               updatingTextBoxes.value = true;

               try {
                  MarkerData markerData = MarkerEntityManipulator.getActiveMarkerData();
                  Vec3 delta = Minecraft.getInstance().cameraEntity.getEyePosition().subtract(markerData.position());
                  float yaw = Mth.wrapDegrees((float)(Mth.atan2(delta.z, delta.x) * 180.0F / (float)Math.PI) - 90.0F);
                  yaw = Math.round(yaw / 15.0F) * 15;
                  this.data.putFloat("yaw", yaw);
                  this.updateNbtField();
               } finally {
                  updatingTextBoxes.value = false;
               }

               this.clearFocus();
            }
         }
      }).bounds(x, y, (itemWidth - 1) / 2, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal(AxiomI18n.get("axiom.hardcoded.set_pitch")), button -> {
         if (!this.closeSanityCheck()) {
            if (!updatingTextBoxes.value) {
               updatingTextBoxes.value = true;

               try {
                  MarkerData markerData = MarkerEntityManipulator.getActiveMarkerData();
                  Vec3 delta = Minecraft.getInstance().cameraEntity.getEyePosition().subtract(markerData.position());
                  double horz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                  float pitch = Mth.wrapDegrees((float)(-(Mth.atan2(delta.y, horz) * 180.0F / (float)Math.PI)));
                  pitch = Math.round(pitch / 15.0F) * 15;
                  this.data.putFloat("pitch", pitch);
                  this.updateNbtField();
               } finally {
                  updatingTextBoxes.value = false;
               }

               this.clearFocus();
            }
         }
      }).bounds(x + itemWidth / 2 + 1, y, (itemWidth - 1) / 2, 20).build());
      x = this.leftPos + 8 + itemWidth + 4;
      y = this.topPos + 17;
      int var20 = 150;
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(x + 1, y + 1, 0, 0, Component.literal(AxiomI18n.get("axiom.hardcoded.box")).withStyle(Style.EMPTY.withColor(4210752)), this.font)
      );
      y += 10;
      ListTag min = NbtHelper.getList(this.data, "min", 6);
      ListTag max = NbtHelper.getList(this.data, "max", 6);
      ListTag minString = NbtHelper.getList(this.data, "min", 8);
      ListTag maxString = NbtHelper.getList(this.data, "max", 8);

      for (int i = 0; i < 6; i++) {
         int finalI = i;
         String minOrMax = i < 3 ? "min" : "max";
         int coordIndex = i % 3;
         if (i == 3) {
            y += 22;
         }

         this.regionEditBox[i] = new EditBox(this.font, x + 50 * (i % 3), y, 49, 20, Component.literal(AxiomI18n.get("axiom.hardcoded.region")));
         if (i < 3) {
            if (min.size() == 3) {
               this.regionEditBox[i].setValue(String.format("%.2f", VersionUtilsNbt.helperListTagGetDoubleOr(min, coordIndex, 0.0)));
            } else if (minString.size() == 3) {
               this.regionEditBox[i].setValue(VersionUtilsNbt.helperListTagGetStringOr(minString, coordIndex, ""));
            }
         } else if (max.size() == 3) {
            this.regionEditBox[i].setValue(String.format("%.2f", VersionUtilsNbt.helperListTagGetDoubleOr(max, coordIndex, 0.0)));
         } else if (maxString.size() == 3) {
            this.regionEditBox[i].setValue(VersionUtilsNbt.helperListTagGetStringOr(maxString, coordIndex, ""));
         }

         this.regionEditBox[i].setResponder(string -> {
            try {
               if (!this.closeSanityCheck()) {
                  if (!updatingTextBoxes.value) {
                     updatingTextBoxes.value = true;
                     string = string.trim();
                     if (string.isEmpty()) {
                        boolean allEmpty = true;

                        for (int k = 0; k < 6; k++) {
                           if (k != finalI && !this.regionEditBox[k].getValue().isEmpty()) {
                              allEmpty = false;
                              break;
                           }
                        }

                        if (allEmpty) {
                           this.data.remove("min");
                           this.data.remove("max");
                           this.updateNbtField();
                        } else if (this.data.contains(minOrMax)) {
                           ListTag listTag = NbtHelper.getList(this.data, minOrMax, 6);
                           if (listTag.size() == 3) {
                              listTag.set(coordIndex, DoubleTag.valueOf(0.0));
                           } else {
                              listTag = NbtHelper.getList(this.data, minOrMax, 8);
                              if (listTag.size() == 3) {
                                 listTag.set(coordIndex, StringTag.valueOf("~0.0"));
                              }
                           }

                           this.updateNbtField();
                           return;
                        }
                     } else {
                        boolean relative = false;
                        if (string.startsWith("~")) {
                           relative = true;
                           string = string.substring(1);
                        }

                        double value;
                        try {
                           value = Double.parseDouble(string);
                           this.regionEditBox[finalI].setTextColor(-1);
                        } catch (Exception var22) {
                           this.regionEditBox[finalI].setTextColor(-65536);
                           return;
                        }

                        if (relative) {
                           if (this.data.contains(minOrMax)) {
                              ListTag listTag = NbtHelper.getList(this.data, minOrMax, 8);
                              if (listTag.size() == 3) {
                                 listTag.set(coordIndex, StringTag.valueOf("~" + value));
                                 this.updateNbtField();
                                 return;
                              }

                              listTag = NbtHelper.getList(this.data, minOrMax, 6);
                              if (listTag.size() == 3) {
                                 ListTag listStringTag = new ListTag();
                                 listStringTag.add(StringTag.valueOf(String.valueOf(listTag.getDouble(0))));
                                 listStringTag.add(StringTag.valueOf(String.valueOf(listTag.getDouble(1))));
                                 listStringTag.add(StringTag.valueOf(String.valueOf(listTag.getDouble(2))));
                                 listStringTag.set(coordIndex, StringTag.valueOf("~" + value));
                                 this.data.put(minOrMax, listStringTag);
                                 this.updateNbtField();
                                 return;
                              }
                           }

                           ListTag listTagx = new ListTag();
                           listTagx.add(StringTag.valueOf("~0.0"));
                           listTagx.add(StringTag.valueOf("~0.0"));
                           listTagx.add(StringTag.valueOf("~0.0"));
                           listTagx.set(coordIndex, StringTag.valueOf("~" + value));
                           this.data.put(minOrMax, listTagx);
                           this.updateNbtField();
                        } else {
                           if (this.data.contains(minOrMax)) {
                              ListTag listTagx = NbtHelper.getList(this.data, minOrMax, 6);
                              if (listTagx.size() == 3) {
                                 listTagx.set(coordIndex, DoubleTag.valueOf(value));
                                 this.updateNbtField();
                                 return;
                              }

                              listTagx = NbtHelper.getList(this.data, minOrMax, 8);
                              if (listTagx.size() == 3) {
                                 boolean hasOtherRelative = false;

                                 for (int j = 0; j < 3; j++) {
                                    if (j != coordIndex && VersionUtilsNbt.helperListTagGetStringOr(listTagx, j, "").trim().startsWith("~")) {
                                       hasOtherRelative = true;
                                       break;
                                    }
                                 }

                                 if (hasOtherRelative) {
                                    listTagx.set(coordIndex, StringTag.valueOf(String.valueOf(value)));
                                 } else {
                                    ListTag listDoubleTag = new ListTag();

                                    try {
                                       listDoubleTag.add(DoubleTag.valueOf(Double.parseDouble(VersionUtilsNbt.helperListTagGetStringOr(listTagx, 0, ""))));
                                    } catch (NumberFormatException var21x) {
                                       listDoubleTag.add(DoubleTag.valueOf(0.0));
                                    }

                                    try {
                                       listDoubleTag.add(DoubleTag.valueOf(Double.parseDouble(VersionUtilsNbt.helperListTagGetStringOr(listTagx, 1, ""))));
                                    } catch (NumberFormatException var20x) {
                                       listDoubleTag.add(DoubleTag.valueOf(0.0));
                                    }

                                    try {
                                       listDoubleTag.add(DoubleTag.valueOf(Double.parseDouble(VersionUtilsNbt.helperListTagGetStringOr(listTagx, 2, ""))));
                                    } catch (NumberFormatException var19x) {
                                       listDoubleTag.add(DoubleTag.valueOf(0.0));
                                    }

                                    listDoubleTag.set(coordIndex, DoubleTag.valueOf(value));
                                    this.data.put(minOrMax, listDoubleTag);
                                 }

                                 this.updateNbtField();
                                 return;
                              }
                           }

                           ListTag listTagxx = new ListTag();
                           listTagxx.add(DoubleTag.valueOf(0.0));
                           listTagxx.add(DoubleTag.valueOf(0.0));
                           listTagxx.add(DoubleTag.valueOf(0.0));
                           listTagxx.set(coordIndex, DoubleTag.valueOf(value));
                           this.data.put(minOrMax, listTagxx);
                           this.updateNbtField();
                        }
                     }
                  }
               }
            } finally {
               updatingTextBoxes.value = false;
            }
         });
         this.addRenderableWidget(this.regionEditBox[i]);
      }

      x = this.leftPos + 8;
      y += 22;
      Component name = Component.literal(AxiomI18n.get("axiom.hardcoded.nbt_data"));
      this.dataEditBox = new MultiLineEditBox(this.font, x, y, 296, 110, name, name);
      this.updateNbtField();
      this.dataEditBox.setValueListener(string -> {
         if (!this.closeSanityCheck()) {
            this.syntaxException = null;

            try {
               if (!updatingTextBoxes.value) {
                  updatingTextBoxes.value = true;
                  this.data = this.hiddenData.copy().merge(TagParser.parseTag(string));
                  this.nameEditBox.setValue(VersionUtilsNbt.helperCompoundTagGetStringOr(this.data, "name", ""));
                  return;
               }
            } catch (CommandSyntaxException var8x) {
               this.syntaxException = var8x;
               return;
            } catch (Exception var9x) {
               var9x.printStackTrace();
               return;
            } finally {
               updatingTextBoxes.value = false;
            }
         }
      });
      this.addRenderableWidget(this.dataEditBox);
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> Minecraft.getInstance().setScreen(null))
            .bounds(this.leftPos + 8, this.topPos + this.menuHeight - 20 - 8, 147, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(this.leftPos + 8 + 148, this.topPos + this.menuHeight - 20 - 8, 147, 20)
            .build()
      );
   }

   public void onClose() {
      super.onClose();
      if (!this.closeSanityCheck()) {
         CompoundTag data = this.data.copy();
         data.putBoolean("axiom:modify", true);
         CompoundTag tag = new CompoundTag();
         tag.put("data", data);
         new AxiomServerboundManipulateEntity(List.of(new AxiomServerboundManipulateEntity.ManipulateEntry(this.editingEntityUuid, null, tag))).send();
      }
   }

   private void updateNbtField() {
      CompoundTag tag = this.data;
      if (!this.hiddenData.isEmpty()) {
         this.hiddenData = new CompoundTag();
      }

      if (this.data.contains("axiom:hide")) {
         ListTag hiddenTags = NbtHelper.getList(this.data, "axiom:hide", 8);
         if (!hiddenTags.isEmpty()) {
            tag = tag.copy();

            for (Tag hiddenKey : hiddenTags) {
               String hiddenKeyStr = VersionUtilsNbt.helperTagAsString(hiddenKey).orElse("");
               Tag hidden = tag.get(hiddenKeyStr);
               if (hidden != null) {
                  tag.remove(hiddenKeyStr);
                  this.hiddenData.put(hiddenKeyStr, hidden);
               }
            }

            tag.remove("axiom:hide");
            this.hiddenData.put("axiom:hide", hiddenTags);
         }
      }

      this.dataEditBox.setValue(new SnbtPrinterTagVisitor().visit(tag));
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, this.menuWidth, this.menuHeight);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      if (!this.closeSanityCheck()) {
         int y = this.topPos + 6;
         guiGraphics.drawString(this.font, this.title, this.leftPos + 8, y, -12566464, false);
         if (this.syntaxException != null) {
            String message = this.syntaxException.getRawMessage().getString();
            message = message + " at position " + this.syntaxException.getCursor();
            List<FormattedCharSequence> lines = this.font.split(Component.literal(message), this.menuWidth - 32);
            y += 175 + (31 - 9 * lines.size()) / 2;

            for (FormattedCharSequence line : lines) {
               guiGraphics.drawString(this.font, line, this.leftPos + 16, y, -3407872, false);
               y += 9;
            }
         }
      }
   }

   private boolean closeSanityCheck() {
      MarkerData markerData = MarkerEntityManipulator.getActiveMarkerData();
      if (markerData != null && markerData.uuid().equals(this.editingEntityUuid)) {
         return false;
      } else {
         Minecraft.getInstance().setScreen(null);
         return true;
      }
   }
}
