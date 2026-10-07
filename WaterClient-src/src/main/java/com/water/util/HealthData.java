package com.water.util;

public record HealthData(
   int baseHeartCount, int fullHearts, boolean halfHeart, int emptyHearts, int absorptionFullHearts, boolean absorptionHalfHeart, int totalWidth
) {
}
