package com.water.util;

import net.minecraft.util.math.ChunkPos;

public record ChunkSuspicion(ChunkPos chunkPos, int suspicionLevel, ChunkPos baseChunk, boolean extreme, boolean source, int maxVineLength) {
}
