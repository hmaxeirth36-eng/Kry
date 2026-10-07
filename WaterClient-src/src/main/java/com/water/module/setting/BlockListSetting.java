package com.water.module.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class BlockListSetting extends Setting<Set<Block>> {
   public final List<Block> availableBlocks = Registries.BLOCK
      .stream()
      .filter(var0 -> var0 != Blocks.AIR)
      .sorted(Comparator.comparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER))
      .toList();
   public long version;

   public BlockListSetting(String text, Block... block) {
      super(text, toSet(block));
   }

   public void setValue(Set<Block> set) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (set != null) {
         for (Block block : set) {
            if (block != null && block != Blocks.AIR) {
               linkedHashSet.add(block);
            }
         }
      }

      super.setValue(linkedHashSet);
      this.version++;
   }

   public boolean contains(Block block) {
      return block != null && this.getValue().contains(block);
   }

   public void toggle(Block block) {
      if (block != null && block != Blocks.AIR) {
         LinkedHashSet linkedHashSet = new LinkedHashSet<>(this.getValue());
         if (!linkedHashSet.add(block)) {
            linkedHashSet.remove(block);
         }

         this.setValue(linkedHashSet);
      }
   }

   public void clear() {
      if (!this.getValue().isEmpty()) {
         this.setValue(Collections.emptySet());
      }
   }

   public int size() {
      return this.getValue().size();
   }

   public long getVersion() {
      return this.version;
   }

   public Set<Block> getSelectedBlocks() {
      return Collections.unmodifiableSet(this.getValue());
   }

   public List<Block> getAvailableBlocks() {
      return this.availableBlocks;
   }

   public List<Block> filter(String text) {
      String s = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
      if (s.isEmpty()) {
         return this.availableBlocks;
      } else {
         ArrayList arrayList = new ArrayList();

         for (Block block : this.availableBlocks) {
            String s1 = this.getDisplayName(block).toLowerCase(Locale.ROOT);
            Identifier identifier = Registries.BLOCK.getId(block);
            String s2 = identifier == null ? "" : identifier.toString().toLowerCase(Locale.ROOT);
            if (s1.contains(s) || s2.contains(s)) {
               arrayList.add(block);
            }
         }

         return arrayList;
      }
   }

   public String getDisplayName(Block block) {
      try {
         return block.getName().getString();
      } catch (Exception exception) {
         Identifier identifier = Registries.BLOCK.getId(block);
         return identifier == null ? "Block" : identifier.getPath();
      }
   }

   public String getSummary() {
      if (this.getValue().isEmpty()) {
         return "None";
      } else {
         Block block = this.getValue().iterator().next();
         String s = this.getDisplayName(block);
         int i = this.getValue().size() - 1;
         return i > 0 ? s + " +" + i : s;
      }
   }

   public static Set<Block> toSet(Block... block) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (block != null) {
         Collections.addAll(linkedHashSet, block);
         linkedHashSet.remove(null);
         linkedHashSet.remove(Blocks.AIR);
      }

      return linkedHashSet;
   }
}
