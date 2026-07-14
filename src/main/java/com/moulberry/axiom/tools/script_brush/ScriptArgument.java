package com.moulberry.axiom.tools.script_brush;

import com.moulberry.axiom.DefaultBlocks;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.mask.LuaHelper;
import imgui.moulberry92.ImGui;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ScriptArgument {
   public String name;
   public ScriptArgument.Type type;
   public Object value;
   public String category = null;
   private float floatMin = 0.0F;
   private float floatMax = 1.0F;
   private int intMin = 0;
   private int intMax = 64;

   public ScriptArgument(String name, ScriptArgument.Type type, Object value) {
      this.name = name;
      this.type = type;
      this.value = value;
   }

   public static ScriptArgument parse(String id, String args) {
      if (id == null) {
         return null;
      } else {
         String var2 = id.toLowerCase(Locale.ROOT);
         switch (var2) {
            case "blockstate": {
               String name = "Block";
               BlockState defaultBlock = Blocks.STONE.defaultBlockState();
               if (args != null) {
                  String[] splitxxx = args.split(",");
                  if (splitxxx.length >= 1) {
                     name = splitxxx[0].trim();
                  }

                  if (splitxxx.length >= 2) {
                     ResourceLocation location = ResourceLocation.tryParse(splitxxx[1].trim());
                     if (location != null) {
                        Optional<Reference<Block>> holder = BuiltInRegistries.BLOCK.getHolder(ResourceKey.create(Registries.BLOCK, location));
                        if (holder.isPresent()) {
                           defaultBlock = DefaultBlocks.forBlock((Block)holder.get().value());
                        }
                     }
                  }
               }

               return new ScriptArgument(name, ScriptArgument.Type.BLOCK_STATE, defaultBlock);
               }
            case "boolean": {
               String name = "Boolean";
               boolean defaultValue = false;
               if (args != null) {
                  String[] splitxx = args.split(",");
                  if (splitxx.length >= 1) {
                     name = splitxx[0].trim();
                  }

                  if (splitxx.length >= 2) {
                     defaultValue = splitxx[1].trim().equalsIgnoreCase("true");
                  }
               }

               return new ScriptArgument(name, ScriptArgument.Type.BOOLEAN, defaultValue);
               }
            case "float": {
               String name = "Float";
               float defaultValue = 0.5F;
               float floatMin = 0.0F;
               float floatMax = 1.0F;
               if (args != null) {
                  String[] splitx = args.split(",");
                  if (splitx.length >= 1) {
                     name = splitx[0].trim();
                  }

                  if (splitx.length >= 2) {
                     try {
                        defaultValue = Float.parseFloat(splitx[1].trim());
                     } catch (NumberFormatException var15) {
                     }

                     floatMin = Math.min(floatMin, defaultValue);
                     floatMax = Math.max(floatMax, defaultValue);
                  }

                  if (splitx.length >= 3) {
                     try {
                        floatMin = Float.parseFloat(splitx[2].trim());
                     } catch (NumberFormatException var14) {
                     }
                  }

                  if (splitx.length >= 4) {
                     try {
                        floatMax = Float.parseFloat(splitx[3].trim());
                     } catch (NumberFormatException var13) {
                     }
                  }
               }

               ScriptArgument scriptArgument = new ScriptArgument(name, ScriptArgument.Type.FLOAT, defaultValue);
               scriptArgument.floatMin = floatMin;
               scriptArgument.floatMax = floatMax;
               return scriptArgument;
               }
            case "int": {
               String name = "Integer";
               int defaultValue = 0;
               int intMin = 0;
               int intMax = 64;
               if (args != null) {
                  String[] split = args.split(",");
                  if (split.length >= 1) {
                     name = split[0].trim();
                  }

                  if (split.length >= 2) {
                     try {
                        defaultValue = Integer.parseInt(split[1].trim());
                     } catch (NumberFormatException var12) {
                     }

                     intMin = Math.min(intMin, defaultValue);
                     intMax = Math.max(intMax, defaultValue);
                  }

                  if (split.length >= 3) {
                     try {
                        intMin = Integer.parseInt(split[2].trim());
                     } catch (NumberFormatException var11) {
                     }
                  }

                  if (split.length >= 4) {
                     try {
                        intMax = Integer.parseInt(split[3].trim());
                     } catch (NumberFormatException var10) {
                     }
                  }
               }

               ScriptArgument scriptArgument = new ScriptArgument(name, ScriptArgument.Type.INT, defaultValue);
               scriptArgument.intMin = intMin;
               scriptArgument.intMax = intMax;
               return scriptArgument;
               }
            default: {
               return null;
               }
         }
      }
   }

   public void displayImgui(SelectBlockWidget selectBlockWidget, int id) {
      switch (this.type) {
         case BLOCK_STATE:
            CustomBlockState blockState = (CustomBlockState)this.value;
            this.value = ImGuiHelper.blockStateWidget(selectBlockWidget, blockState, this.name, id);
            break;
         case BOOLEAN:
            boolean boolValue = (Boolean)this.value;
            ImGui.pushID(id);
            if (ImGui.checkbox(this.name, boolValue)) {
               this.value = !boolValue;
            }

            ImGui.popID();
            break;
         case FLOAT:
            float[] floatValue = new float[]{(Float)this.value};
            ImGui.pushID(id);
            if (ImGui.sliderFloat(this.name, floatValue, this.floatMin, this.floatMax)) {
               this.value = floatValue[0];
            }

            ImGui.popID();
            break;
         case INT:
            int[] intValue = new int[]{(Integer)this.value};
            ImGui.pushID(id);
            if (ImGui.sliderInt(this.name, intValue, this.intMin, this.intMax)) {
               this.value = intValue[0];
            }

            ImGui.popID();
      }
   }

   public String toLuaString() {
      switch (this.type) {
         case BLOCK_STATE:
            return String.valueOf(LuaHelper.stateToInternalId(((CustomBlockState)this.value).getVanillaState()));
         case BOOLEAN:
         case FLOAT:
         case INT:
            return String.valueOf(this.value);
         default:
            return "";
      }
   }

   public static enum Type {
      BLOCK_STATE,
      BOOLEAN,
      FLOAT,
      INT;
   }
}
