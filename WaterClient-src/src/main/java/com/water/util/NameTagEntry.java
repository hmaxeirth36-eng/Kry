package com.water.util;

import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public record NameTagEntry(Object entity, Vec3d labelPos, Text nameLabel, Text healthLabel, List<ItemEntry> items) {
}
