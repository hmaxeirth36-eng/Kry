package com.water.util;

import net.minecraft.entity.player.PlayerEntity;

public record ThreatInfo(boolean hasAnyEnemy, boolean hasCriticalThreat, PlayerEntity threat, double distance) {
}
