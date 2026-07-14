package com.moulberry.axiom.editor.styles;

import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiStyle;
import imgui.moulberry92.ImVec4;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Base64;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class StyleHelper {
   private static final List<StyleHelper.VarGetter1> VAR_GETTERS = List.of(
      ImGuiStyle::getWindowBorderSize,
      ImGuiStyle::getChildBorderSize,
      ImGuiStyle::getPopupBorderSize,
      ImGuiStyle::getFrameBorderSize,
      ImGuiStyle::getTabBorderSize,
      ImGuiStyle::getWindowRounding,
      ImGuiStyle::getChildRounding,
      ImGuiStyle::getPopupRounding,
      ImGuiStyle::getFrameRounding,
      ImGuiStyle::getTabRounding,
      ImGuiStyle::getScrollbarRounding,
      ImGuiStyle::getGrabRounding
   );
   private static final List<StyleHelper.VarSetter1> VAR_SETTERS = List.of(
      ImGuiStyle::setWindowBorderSize,
      ImGuiStyle::setChildBorderSize,
      ImGuiStyle::setPopupBorderSize,
      ImGuiStyle::setFrameBorderSize,
      ImGuiStyle::setTabBorderSize,
      ImGuiStyle::setWindowRounding,
      ImGuiStyle::setChildRounding,
      ImGuiStyle::setPopupRounding,
      ImGuiStyle::setFrameRounding,
      ImGuiStyle::setTabRounding,
      ImGuiStyle::setScrollbarRounding,
      ImGuiStyle::setGrabRounding
   );

   public static StyleHelper.ModifiedStyleValues calcModifiedStyleValues(ImGuiStyle base, ImGuiStyle current) {
      Int2IntMap changedColors = new Int2IntRBTreeMap();
      ImVec4[] baseColors = base.getColors();
      ImVec4[] currentColors = current.getColors();

      for (int i = 0; i < baseColors.length; i++) {
         int baseColor = ImGui.colorConvertFloat4ToU32(baseColors[i]);
         int currentColor = ImGui.colorConvertFloat4ToU32(currentColors[i]);
         if (baseColor != currentColor) {
            changedColors.put(i, currentColor);
         }
      }

      Int2FloatMap changedVars = new Int2FloatRBTreeMap();

      for (int ix = 0; ix < VAR_GETTERS.size(); ix++) {
         StyleHelper.VarGetter1 getter = VAR_GETTERS.get(ix);
         float baseValue = getter.get(base);
         float currentValue = getter.get(current);
         if (baseValue != currentValue) {
            changedVars.put(ix, currentValue);
         }
      }

      return new StyleHelper.ModifiedStyleValues(changedColors, changedVars);
   }

   public static void applyModifiedStyleValues(ImGuiStyle current, StyleHelper.ModifiedStyleValues modifiedStyleValues) {
      ObjectIterator var2 = modifiedStyleValues.changedColors.int2IntEntrySet().iterator();

      while (var2.hasNext()) {
         Entry entry = (Entry)var2.next();
         current.setColor(entry.getIntKey(), entry.getIntValue());
      }

      var2 = modifiedStyleValues.changedVars.int2FloatEntrySet().iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry entry = (it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry)var2.next();
         VAR_SETTERS.get(entry.getIntKey()).set(current, entry.getFloatValue());
      }
   }

   public static void copyStyleValues(ImGuiStyle base, ImGuiStyle current) {
      current.setColors(base.getColors());
      current.setWindowBorderSize(base.getWindowBorderSize());
      current.setChildBorderSize(base.getChildBorderSize());
      current.setPopupBorderSize(base.getPopupBorderSize());
      current.setFrameBorderSize(base.getFrameBorderSize());
      current.setTabBorderSize(base.getTabBorderSize());
      current.setWindowRounding(base.getWindowRounding());
      current.setChildRounding(base.getChildRounding());
      current.setPopupRounding(base.getPopupRounding());
      current.setFrameRounding(base.getFrameRounding());
      current.setTabRounding(base.getTabRounding());
      current.setScrollbarRounding(base.getScrollbarRounding());
      current.setGrabRounding(base.getGrabRounding());
   }

   public record ModifiedStyleValues(Int2IntMap changedColors, Int2FloatMap changedVars) {
      public void write(FriendlyByteBuf byteBuf) {
         byteBuf.writeVarInt(this.changedColors.size());
         ObjectIterator var2 = this.changedColors.int2IntEntrySet().iterator();

         while (var2.hasNext()) {
            Entry entry = (Entry)var2.next();
            byteBuf.writeVarInt(entry.getIntKey());
            byteBuf.writeInt(entry.getIntValue());
         }

         byteBuf.writeVarInt(this.changedVars.size());
         var2 = this.changedVars.int2FloatEntrySet().iterator();

         while (var2.hasNext()) {
            it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry entry = (it.unimi.dsi.fastutil.ints.Int2FloatMap.Entry)var2.next();
            byteBuf.writeVarInt(entry.getIntKey());
            byteBuf.writeFloat(entry.getFloatValue());
         }
      }

      @Nullable
      public static StyleHelper.ModifiedStyleValues read(FriendlyByteBuf byteBuf) {
         try {
            Int2IntMap changedColors = new Int2IntRBTreeMap();
            int changedColorCount = byteBuf.readVarInt();

            for (int i = 0; i < changedColorCount; i++) {
               changedColors.put(byteBuf.readVarInt(), byteBuf.readInt());
            }

            Int2FloatMap changedVars = new Int2FloatRBTreeMap();
            int changedVarCount = byteBuf.readVarInt();

            for (int i = 0; i < changedVarCount; i++) {
               changedVars.put(byteBuf.readVarInt(), byteBuf.readFloat());
            }

            return new StyleHelper.ModifiedStyleValues(changedColors, changedVars);
         } catch (Exception var6) {
            return null;
         }
      }
   }

   public record Theme(String name, String baseTheme, StyleHelper.ModifiedStyleValues values) {
      public String convertToBase64() {
         FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
         friendlyByteBuf.writeShort(31325);
         friendlyByteBuf.writeByte(0);
         friendlyByteBuf.writeUtf(this.name, 64);
         friendlyByteBuf.writeUtf(this.baseTheme, 64);
         this.values.write(friendlyByteBuf);
         byte[] bytes = new byte[friendlyByteBuf.writerIndex()];
         friendlyByteBuf.getBytes(0, bytes);
         return "AS" + Base64.getEncoder().encodeToString(bytes);
      }

      @Nullable
      public static StyleHelper.Theme convertFromBase64(String base64) {
         try {
            if (!base64.startsWith("AS")) {
               return null;
            } else {
               base64 = base64.substring(2);
               byte[] bytes = Base64.getDecoder().decode(base64);
               FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
               if (byteBuf.readShort() != 31325) {
                  return null;
               } else {
                  byteBuf.readByte();
                  String name = byteBuf.readUtf(64);
                  String baseTheme = byteBuf.readUtf(64);
                  StyleHelper.ModifiedStyleValues modifiedStyleValues = StyleHelper.ModifiedStyleValues.read(byteBuf);
                  return modifiedStyleValues == null ? null : new StyleHelper.Theme(name, baseTheme, modifiedStyleValues);
               }
            }
         } catch (Exception var6) {
            return null;
         }
      }
   }

   public interface VarGetter1 {
      float get(ImGuiStyle var1);
   }

   public interface VarSetter1 {
      void set(ImGuiStyle var1, float var2);
   }
}
