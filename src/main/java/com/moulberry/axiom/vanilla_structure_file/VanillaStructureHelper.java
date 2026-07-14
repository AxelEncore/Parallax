package com.moulberry.axiom.vanilla_structure_file;

import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.hooks.StructureTemplateExt;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class VanillaStructureHelper {
   public static CompoundTag toStructureNbt(ClipboardObject clipboardObject) {
      StructureTemplate structureTemplate = new StructureTemplate();
      structureTemplate.setAuthor(Minecraft.getInstance().player.getName().getString());
      ((StructureTemplateExt)structureTemplate).axiom$fillFromClipboard(clipboardObject);
      return structureTemplate.save(new CompoundTag());
   }
}
