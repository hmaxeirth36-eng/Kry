package com.water.module.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class EntityListSetting extends Setting<Set<EntityType<?>>> {
   public final List<EntityType<?>> availableMobs = Registries.ENTITY_TYPE
      .stream()
      .filter(EntityListSetting::isSelectableMob)
      .sorted(Comparator.comparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER))
      .toList();
   public long version;

   public EntityListSetting(String text, EntityType<?>... entityType) {
      super(text, toSet(entityType));
   }

   public static boolean isSelectableMob(EntityType<?> entityType) {
      SpawnGroup spawnGroup = entityType.getSpawnGroup();
      return spawnGroup == SpawnGroup.MONSTER
         || spawnGroup == SpawnGroup.CREATURE
         || spawnGroup == SpawnGroup.AMBIENT
         || spawnGroup == SpawnGroup.AXOLOTLS
         || spawnGroup == SpawnGroup.UNDERGROUND_WATER_CREATURE
         || spawnGroup == SpawnGroup.WATER_CREATURE
         || spawnGroup == SpawnGroup.WATER_AMBIENT;
   }

   public void setValue(Set<EntityType<?>> var1) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (var1 != null) {
         for (EntityType entitytype : var1) {
            if (entitytype != null) {
               linkedHashSet.add(entitytype);
            }
         }
      }

      super.setValue(linkedHashSet);
      this.version++;
   }

   public boolean contains(EntityType<?> entityType) {
      return entityType != null && this.getValue().contains(entityType);
   }

   public void toggle(EntityType<?> entityType) {
      if (entityType != null) {
         LinkedHashSet linkedHashSet = new LinkedHashSet<>(this.getValue());
         if (!linkedHashSet.add(entityType)) {
            linkedHashSet.remove(entityType);
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

   public Set<EntityType<?>> getSelectedMobs() {
      return Collections.unmodifiableSet(this.getValue());
   }

   public List<EntityType<?>> getAvailableMobs() {
      return this.availableMobs;
   }

   public List<EntityType<?>> filter(String text) {
      String s = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
      if (s.isEmpty()) {
         return this.availableMobs;
      } else {
         ArrayList arrayList = new ArrayList();

         for (EntityType entitytype : this.availableMobs) {
            String s1 = this.getDisplayName(entitytype).toLowerCase(Locale.ROOT);
            Identifier identifier = Registries.ENTITY_TYPE.getId(entitytype);
            String s2 = identifier == null ? "" : identifier.toString().toLowerCase(Locale.ROOT);
            if (s1.contains(s) || s2.contains(s)) {
               arrayList.add(entitytype);
            }
         }

         return arrayList;
      }
   }

   public String getDisplayName(EntityType<?> entityType) {
      try {
         return entityType.getName().getString();
      } catch (Exception exception) {
         Identifier identifier = Registries.ENTITY_TYPE.getId((EntityType<?>)entityType);
         return identifier == null ? "Mob" : identifier.getPath();
      }
   }

   public String getSummary() {
      if (this.getValue().isEmpty()) {
         return "None";
      } else {
         EntityType entitytype = this.getValue().iterator().next();
         String s = this.getDisplayName(entitytype);
         int i = this.getValue().size() - 1;
         return i > 0 ? s + " +" + i : s;
      }
   }

   public static Set<EntityType<?>> toSet(EntityType<?>... entityType) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (entityType != null) {
         Collections.addAll(linkedHashSet, entityType);
         linkedHashSet.remove(null);
      }

      return linkedHashSet;
   }
}
