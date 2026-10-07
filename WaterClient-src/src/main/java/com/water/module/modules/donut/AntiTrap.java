package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity.RemovalReason;

public final class AntiTrap extends Module {
   public final Setting<Boolean> armorStands = new Setting<>("Armor Stands", true);
   public final Setting<Boolean> minecarts = new Setting<>("Minecarts", true);
   public final Setting<Boolean> chestMinecarts = new Setting<>("Chest Minecarts", true);
   public final Setting<Boolean> hopperMinecarts = new Setting<>("Hopper Minecarts", true);

   public AntiTrap() {
      super("AntiTrap", Category.DONUT);
      this.addSetting(this.armorStands);
      this.addSetting(this.minecarts);
      this.addSetting(this.chestMinecarts);
      this.addSetting(this.hopperMinecarts);
   }

   @Override
   public void onEnable() {
      this.removeTrapEntities();
   }

   @Override
   public void onTick() {
      this.removeTrapEntities();
   }

   public void removeTrapEntities() {
      if (mc.world != null) {
         ArrayList<Entity> arrayList = new ArrayList<>();
         mc.world.getEntities().forEach(var2 -> {
            if (var2 != null && this.isTrapEntity(var2.getType())) {
               arrayList.add(var2);
            }
         });
         arrayList.forEach(var0 -> {
            if (!var0.isRemoved()) {
               var0.remove(RemovalReason.DISCARDED);
            }
         });
      }
   }

   public boolean isTrapEntity(EntityType<?> entityType) {
      if (entityType == null) {
         return false;
      } else if (this.armorStands.getValue() && entityType.equals(EntityType.ARMOR_STAND)) {
         return true;
      } else if (this.minecarts.getValue() && entityType.equals(EntityType.MINECART)) {
         return true;
      } else {
         return this.chestMinecarts.getValue() && entityType.equals(EntityType.CHEST_MINECART)
            ? true
            : this.hopperMinecarts.getValue() && entityType.equals(EntityType.HOPPER_MINECART);
      }
   }
}
