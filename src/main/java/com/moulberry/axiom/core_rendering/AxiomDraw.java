package com.moulberry.axiom.core_rendering;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record AxiomDraw(AxiomDrawBuffer drawBuffer, @Nullable Matrix4f modelViewMatrix, @Nullable Vector3f modelOffset, @Nullable BlockPos chunkOffset) {
}
