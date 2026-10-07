package com.water.util;

import java.util.List;
import net.minecraft.text.Text;

public record TabListData(Text title, List<ScoreLine> lines, String signature) {
}
