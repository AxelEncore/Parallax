package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.palette.EditorPalette;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spongepowered.configurate.CommentedConfigurationNode;

class InternalConfigurationLegacy extends LegacyAbstractConfigurationCategory {
   public long nextUpdateNag = this.load(Long.class, "nextUpdateNag", 0L);
   public boolean shownIntroduction = this.load(Boolean.class, "shownIntroduction", false);
   public Set<String> completedTutorials = new LinkedHashSet<>(this.load(List.class, "completedTutorials", List.of()));
   public List<String> openEditorWindowTypes = this.load(List.class, "openEditorWindowTypes", EditorWindowType.getOpenByName());
   public String defaultLayout = this.load(
      String.class,
      "defaultLayout",
      "[Window][###Tools]\nPos=0,0\nSize=300,250\nCollapsed=0\nDockId=0x00000003,0\n\n[Window][###Tool Options]\nPos=0,250\nSize=300,750\nCollapsed=0\nDockId=0x00000004,0\n\n[Window][###Clipboard]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,0\n\n[Window][###TargetInfo]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,1\n\n[Window][###Palette]\nPos=1700,200\nSize=300,200\nCollapsed=0\nDockId=0x0000000E,0\n\n[Window][###ActiveBlock]\nPos=1700,400\nSize=300,100\nCollapsed=0\nDockId=0x0000000C,0\n\n[Window][###History]\nPos=1700,500\nSize=300,300\nCollapsed=0\nDockId=0x0000000A,0\n\n[Window][###WorldProperties]\nPos=1700,800\nSize=300,200\nCollapsed=0\nDockId=0x00000008,0\n\n[Docking][Data]\nDockSpace           ID=0x8B93E3BD Window=0xA787BDB4 Pos=0,0 Size=2000,1000 Split=X\nDockNode          ID=0x00000005 Parent=0x8B93E3BD SizeRef=1700,1000 Split=X\n DockNode        ID=0x00000001 Parent=0x00000005 SizeRef=300,1000 Split=Y\n   DockNode      ID=0x00000003 Parent=0x00000001 SizeRef=300,250 Selected=0x80AFE82B\n   DockNode      ID=0x00000004 Parent=0x00000001 SizeRef=300,750 Selected=0xECA27DCB\n DockNode        ID=0x00000002 Parent=0x00000005 SizeRef=1400,1000 CentralNode=1 Selected=0x1F1A625A\nDockNode          ID=0x00000006 Parent=0x8B93E3BD SizeRef=300,1000 Split=Y Selected=0x34064FA7\n DockNode        ID=0x00000007 Parent=0x00000006 SizeRef=300,800 Split=Y Selected=0x34064FA7\n   DockNode      ID=0x00000009 Parent=0x00000007 SizeRef=300,500 Split=Y Selected=0x34064FA7\n     DockNode    ID=0x0000000B Parent=0x00000009 SizeRef=300,400 Split=Y Selected=0x34064FA7\n       DockNode  ID=0x0000000D Parent=0x0000000B SizeRef=300,200 Selected=0x34064FA7\n       DockNode  ID=0x0000000E Parent=0x0000000B SizeRef=300,200 Selected=0x1E514AEA\n     DockNode    ID=0x0000000C Parent=0x00000009 SizeRef=300,100 Selected=0x1D216E21\n   DockNode      ID=0x0000000A Parent=0x00000007 SizeRef=300,300 Selected=0xFE0E9DDF\n DockNode        ID=0x00000008 Parent=0x00000006 SizeRef=300,200 Selected=0x602D8B84"
   );
   public int lastTranslationCount = this.load(Number.class, "lastTranslationCount", 0).intValue();
   public float globalScale = this.load(Number.class, "globalScale", 1.0F).floatValue();
   public String savedCustomTheme = this.load(String.class, "savedCustomTheme", "");
   public boolean showCloseWindowButton = this.load(Boolean.class, "showCloseWindowButton", false);
   public boolean showToolMaskOpenWarning = this.load(Boolean.class, "showToolMaskOpenWarning", true);
   public boolean showNon90DegreeRotationWarning = this.load(Boolean.class, "showNon90DegreeRotationWarning", true);
   public boolean showQuickReplaceCtrlClickTip = this.load(Boolean.class, "showQuickReplaceCtrlClickTip", true);
   public boolean dockedInventoryWithPalette = this.load(Boolean.class, "dockedInventoryWithPalette", false);
   public EditorPalette rootEditorPalette;
   public Map<String, String> customDowngradeSuggestions;

   public InternalConfigurationLegacy(CommentedConfigurationNode node) {
      super(node);
      CommentedConfigurationNode rootEditorPaletteNode = (CommentedConfigurationNode)this.node.node(new Object[]{"rootEditorPalette"});

      try {
         this.rootEditorPalette = EditorPalette.loadFromLegacyConfig("", rootEditorPaletteNode);
      } catch (Exception var7) {
         this.rootEditorPalette = new EditorPalette("");
      }

      List<String> customDowngradeSuggestions = this.load(List.class, "customDowngradeSuggestions", List.of());
      this.customDowngradeSuggestions = new LinkedHashMap<>();

      for (String suggestion : customDowngradeSuggestions) {
         String[] split = suggestion.split("->");
         if (split.length == 2) {
            this.customDowngradeSuggestions.put(split[0], split[1]);
         }
      }
   }

   @Override
   public void applyToNewConfiguration(AxiomConfig newConfiguration) {
      newConfiguration.internal.nextUpdateNag = this.nextUpdateNag;
      newConfiguration.internal.shownIntroduction = this.shownIntroduction;
      newConfiguration.internal.completedTutorials = this.completedTutorials;
      newConfiguration.internal.openEditorWindowTypes = this.openEditorWindowTypes;
      newConfiguration.internal.defaultLayout = this.defaultLayout;
      newConfiguration.internal.lastTranslationCount = this.lastTranslationCount;
      newConfiguration.internal.globalScale = this.globalScale;
      newConfiguration.internal.savedCustomTheme = this.savedCustomTheme;
      newConfiguration.internal.showCloseWindowButton = this.showCloseWindowButton;
      newConfiguration.internal.showToolMaskOpenWarning = this.showToolMaskOpenWarning;
      newConfiguration.internal.showNon90DegreeRotationWarning = this.showNon90DegreeRotationWarning;
      newConfiguration.internal.showQuickReplaceCtrlClickTip = this.showQuickReplaceCtrlClickTip;
      newConfiguration.internal.dockedInventoryWithPalette = this.dockedInventoryWithPalette;
      newConfiguration.internal.rootEditorPalette = this.rootEditorPalette;
      newConfiguration.internal.customDowngradeSuggestions = this.customDowngradeSuggestions;
   }
}
