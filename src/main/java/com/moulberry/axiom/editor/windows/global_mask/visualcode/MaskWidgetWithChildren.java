package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MaskWidgetWithChildren extends MaskWidget {
   protected final List<MaskWidget> children = new ArrayList<>();
   private final int maxChildren;
   private final Set<MaskWidget> removeQueue = new HashSet<>();
   private final String name;
   private boolean iterating = false;
   private boolean hovered = false;
   private boolean clicked = false;

   public MaskWidgetWithChildren(String name, int maxChildren) {
      this.name = name;
      this.maxChildren = maxChildren;
   }

   public List<MaskWidget> getChildren() {
      return this.children;
   }

   public void addChild(int index, MaskWidget child) {
      if (index < 0) {
         index = this.children.size() + index + 1;
      }

      MaskWidgetWithChildren oldParent = child.parent();
      if (oldParent == this) {
         int currentIndex = this.children.indexOf(child);
         this.children.remove(currentIndex);
         if (index > currentIndex) {
            index--;
         }
      } else if (oldParent != null) {
         oldParent.children.remove(child);
      }

      child.parent(this);
      this.children.add(index, child);
      ToolMaskWindow.markDirty(this);
   }

   public void removeChild(MaskWidget widget) {
      if (!this.iterating) {
         this.children.remove(widget);
         widget.parent(null);
         ToolMaskWindow.markDirty(this);
      } else {
         this.removeQueue.add(widget);
      }
   }

   protected void renderExtra() {
   }

   @Override
   protected void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      boolean newHovered = false;
      boolean newClicked = false;
      if (this.clicked) {
         ImGuiHelper.pushStyleColor(21, ImGui.getColorU32(23));
      } else if (this.hovered) {
         ImGuiHelper.pushStyleColor(21, ImGui.getColorU32(22));
      }

      ImGui.button(this.name, 80.0F, 0.0F);
      newHovered |= ImGui.isItemHovered();
      newClicked |= ImGui.isItemActive();
      if (this.clicked || this.hovered) {
         ImGuiHelper.popStyleColor();
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
         allowDragDropSource = false;
      }

      this.renderExtra();
      ImVec2 startPos = ImGui.getCursorScreenPos();
      int addIndex = -1;
      MaskWidget addWidget = null;
      ImGui.indent();
      int index = 0;
      float startDragDropY = startPos.y - 10.0F;
      this.iterating = true;

      for (MaskWidget child : this.children) {
         ImVec2 pos = ImGui.getCursorScreenPos();
         index++;
         child.render(allowDragDropSource, allowDragDropTarget);
         if (allowDragDropTarget.value) {
            ImVec2 afterPos = ImGui.getCursorScreenPos();
            float endDragDropY = (pos.y + afterPos.y - ImGui.getStyle().getItemSpacingY()) / 2.0F;
            ImGui.setCursorScreenPos(pos.x, startDragDropY);
            ImGui.dummy(ImGui.getContentRegionAvailX(), endDragDropY - startDragDropY);
            if (this.maxChildren <= 0 || this.children.size() < this.maxChildren) {
               CustomBlockState dragDropped = ImGuiHelper.blockStateDragDropTarget(() -> {
                  allowDragDropTarget.value = false;
                  ImGui.getWindowDrawList().addRect(pos.x, pos.y - 2.0F, pos.x + ImGui.getContentRegionAvailX(), pos.y - 1.0F, ImGui.getColorU32(55));
               });
               if (dragDropped != null) {
                  allowDragDropTarget.value = false;
                  addIndex = index - 1;
                  addWidget = new BlockMaskWidget(dragDropped.getCustomBlock());
               }

               if (allowDragDropTarget.value && ImGui.beginDragDropTarget()) {
                  EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class, 3072);
                  if (droppedPalette != null) {
                     allowDragDropTarget.value = false;
                     ImGui.getWindowDrawList().addRect(pos.x, pos.y - 2.0F, pos.x + ImGui.getContentRegionAvailX(), pos.y - 1.0F, ImGui.getColorU32(55));
                     if (ImGui.isMouseReleased(0)) {
                        List<CustomBlock> customBlocks = new ArrayList<>();

                        for (CustomBlockStateOrTombstone block : droppedPalette.getBlocks()) {
                           if (block instanceof CustomBlockState customBlockState) {
                              customBlocks.add(customBlockState.getCustomBlock());
                           }
                        }

                        if (!customBlocks.isEmpty()) {
                           if (customBlocks.size() == 1) {
                              addIndex = index - 1;
                              addWidget = new BlockMaskWidget(customBlocks.get(0));
                           } else {
                              MaskWidgetWithChildren orMask = new OrMaskWidget();

                              for (CustomBlock customBlock : customBlocks) {
                                 orMask.addChild(-1, new BlockMaskWidget(customBlock));
                              }

                              addIndex = index - 1;
                              addWidget = orMask;
                           }
                        }
                     }
                  }

                  ImGui.endDragDropTarget();
               }
            }

            if (allowDragDropTarget.value && ImGui.beginDragDropTarget()) {
               MaskWidget widget = (MaskWidget)ImGui.acceptDragDropPayload("MaskWidget", 3072);
               if (widget != null && widget.isDragging() && (this.maxChildren <= 0 || widget.parent() == this || this.children.size() < this.maxChildren)) {
                  allowDragDropTarget.value = false;
                  ImGui.getWindowDrawList().addRect(pos.x, pos.y - 2.0F, pos.x + ImGui.getContentRegionAvailX(), pos.y - 1.0F, ImGui.getColorU32(55));
                  if (ImGui.isMouseReleased(0)) {
                     addIndex = index - 1;
                     addWidget = widget;
                     widget.setDragging(false);
                  }
               }

               ImGui.endDragDropTarget();
            }

            startDragDropY = endDragDropY;
            ImGui.setCursorScreenPos(afterPos.x, afterPos.y);
         }
      }

      this.iterating = false;
      if (allowDragDropTarget.value) {
         ImVec2 pos = ImGui.getCursorScreenPos();
         ImGui.setCursorScreenPos(pos.x, startDragDropY);
         ImGui.dummy(ImGui.getContentRegionAvailX(), pos.y + 10.0F - startDragDropY);
         if (this.maxChildren <= 0 || this.children.size() < this.maxChildren) {
            CustomBlockState dragDroppedx = ImGuiHelper.blockStateDragDropTarget(() -> {
               allowDragDropTarget.value = false;
               ImGui.getWindowDrawList().addRect(pos.x, pos.y - 2.0F, pos.x + ImGui.getContentRegionAvailX(), pos.y - 1.0F, ImGui.getColorU32(55));
            });
            if (dragDroppedx != null) {
               allowDragDropTarget.value = false;
               addIndex = -1;
               addWidget = new BlockMaskWidget(dragDroppedx.getCustomBlock());
            }

            if (allowDragDropTarget.value && ImGui.beginDragDropTarget()) {
               EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class, 3072);
               if (droppedPalette != null) {
                  allowDragDropTarget.value = false;
                  ImGui.getWindowDrawList().addRect(pos.x, pos.y - 2.0F, pos.x + ImGui.getContentRegionAvailX(), pos.y - 1.0F, ImGui.getColorU32(55));
                  if (ImGui.isMouseReleased(0)) {
                     List<CustomBlock> customBlocks = new ArrayList<>();

                     for (CustomBlockStateOrTombstone blockx : droppedPalette.getBlocks()) {
                        if (blockx instanceof CustomBlockState customBlockState) {
                           customBlocks.add(customBlockState.getCustomBlock());
                        }
                     }

                     if (!customBlocks.isEmpty()) {
                        if (customBlocks.size() == 1) {
                           addIndex = -1;
                           addWidget = new BlockMaskWidget(customBlocks.get(0));
                        } else {
                           MaskWidgetWithChildren orMask = new OrMaskWidget();

                           for (CustomBlock customBlock : customBlocks) {
                              orMask.addChild(-1, new BlockMaskWidget(customBlock));
                           }

                           addIndex = -1;
                           addWidget = orMask;
                        }
                     }
                  }
               }

               ImGui.endDragDropTarget();
            }
         }

         if (allowDragDropTarget.value && ImGui.beginDragDropTarget()) {
            MaskWidget widget = (MaskWidget)ImGui.acceptDragDropPayload("MaskWidget", 3072);
            if (widget != null && widget.isDragging() && (this.maxChildren <= 0 || widget.parent() == this || this.children.size() < this.maxChildren)) {
               allowDragDropTarget.value = false;
               ImGui.getWindowDrawList().addRect(pos.x, pos.y - 2.0F, pos.x + ImGui.getContentRegionAvailX(), pos.y - 1.0F, ImGui.getColorU32(55));
               if (ImGui.isMouseReleased(0)) {
                  addIndex = -1;
                  addWidget = widget;
                  widget.setDragging(false);
               }
            }

            ImGui.endDragDropTarget();
         }

         ImGui.setCursorScreenPos(pos.x, pos.y);
      }

      if (addWidget != null) {
         this.addChild(addIndex, addWidget);
      }

      this.removeQueue.forEach(widget -> {
         this.children.remove(widget);
         widget.parent(null);
         ToolMaskWindow.markDirty(this);
      });
      this.removeQueue.clear();
      ImGui.unindent();
      ImVec2 endPos = ImGui.getCursorScreenPos();
      float itemSpacingY = ImGui.getStyle().getItemSpacingY();
      ImGui.setCursorScreenPos(startPos.x, startPos.y - itemSpacingY);
      if (this.clicked) {
         ImGuiHelper.pushStyleColor(21, ImGui.getColorU32(23));
      } else if (this.hovered) {
         ImGuiHelper.pushStyleColor(21, ImGui.getColorU32(22));
      }

      ImGui.button("##EmptyButton", ImGui.getStyle().getIndentSpacing() - itemSpacingY, endPos.y - startPos.y + itemSpacingY);
      newHovered |= ImGui.isItemHovered();
      newClicked |= ImGui.isItemActive();
      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
         allowDragDropSource = false;
      }

      ImGui.setCursorScreenPos(endPos.x, endPos.y);
      ImGui.button("##EmptyButton", 80.0F, 0.0F);
      newHovered |= ImGui.isItemHovered();
      newClicked |= ImGui.isItemActive();
      if (this.clicked || this.hovered) {
         ImGuiHelper.popStyleColor();
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
         allowDragDropSource = false;
      }

      this.hovered = newHovered;
      this.clicked = newClicked;
      if (this.hovered) {
         ImGui.setMouseCursor(7);
      }
   }
}
