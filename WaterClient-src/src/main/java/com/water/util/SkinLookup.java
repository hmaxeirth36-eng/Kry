package com.water.util;

import java.util.UUID;
import net.minecraft.entity.player.PlayerSkinType;

public record SkinLookup(String playerName, UUID uuid, String textureUrl, PlayerSkinType skinType) {
}
