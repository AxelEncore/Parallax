package com.moulberry.axiom.services;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiomclientapi.regions.BlockRegion;
import java.text.NumberFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ToolService implements com.moulberry.axiomclientapi.service.ToolService {
   public void pushBlockRegionChange(BlockRegion blockRegion) {
      ChunkedBlockRegion chunkedBlockRegion = (ChunkedBlockRegion)blockRegion;
      String changeCount = NumberFormat.getInstance().format((long)chunkedBlockRegion.count());
      String historyDescription = AxiomI18n.get("axiom.history_description.modified", changeCount);
      RegionHelper.pushBlockRegionChange((ChunkedBlockRegion)blockRegion, historyDescription);
   }

   @Nullable
   public BlockHitResult raycastBlock() {
      RayCaster.RaycastResult raycastResult = Tool.raycastBlock();
      return raycastResult != null ? new BlockHitResult(raycastResult.getLocation(), raycastResult.direction(), raycastResult.blockPos(), false) : null;
   }

   public BlockState getActiveBlock() {
      return Tool.getActiveBlock();
   }

   public Vec3 getLookDirection() {
      return Tool.getLookDirection();
   }

   public boolean isMouseDown(int button) {
      return Tool.isMouseDown(button);
   }

   public ForkJoinPool getSharedPoolThreadExecutor() {
      return Tool.sharedPoolThreadExecutor;
   }

   public ExecutorService getSharedSingleThreadExecutor() {
      return Tool.sharedSingleThreadExecutor;
   }

   public void acquireChunkRenderOverrider(String id) {
      ChunkRenderOverrider.acquire(id);
   }

   public void releaseChunkRenderOverrider(String id) {
      ChunkRenderOverrider.release(id);
   }

   public void setBlockRenderOverride(int x, int y, int z, BlockState blockState) {
      ChunkRenderOverrider.setBlock(x, y, z, blockState);
   }
}
