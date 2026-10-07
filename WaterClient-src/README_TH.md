# WaterClient v5 + คู่มือใช้งาน (ภาษาไทย)

โปรเจกต์นี้คือ **WaterClient** ที่ Sus Chunk / Storage ESP / ClickGUI ทำงานได้จริงบน **Minecraft 1.21.11 Fabric**

## ทำไม Claude Client ถึง Sus Chunk ไม่ขึ้น

ใน Minecraft 1.21.11 ระบบ render เปลี่ยนไปมาก  
`WorldRenderEvents.AFTER_TRANSLUCENT` ถูกเอาออก และการวาดด้วย BufferBuilder แบบเก่าไม่เสถียร

WaterClient แก้โดยใช้:
- **Mixin เข้า WorldRenderer**
- **ShapeBatch + Render3D** ของตัวเอง

ดังนั้นให้ใช้ **WaterClient เป็นฐาน** แทน Claude Client เดิม

---

## ฟีเจอร์ที่เกี่ยวกับที่คุณต้องการ

| ฟีเจอร์ | หมวดใน GUI | รายละเอียด |
|---------|------------|------------|
| **SUS Chunk Finder** | DONUT | สแกน amethyst + **ฐานใต้ดิน** (กล่อง/barrel/hopper/shulker/spawner ที่ Y < 0) แล้วไฮไลท์ chunk + แสดง % |
| **Storage ESP** | RENDER | แสดงกล่อง/storage ผ่านกำแพง |
| **Player ESP** | RENDER | แสดงผู้เล่น (รวมใต้ดิน) |
| **No Render** | RENDER | ตัดพาร์ติเคิล/ไฟ ลดแลค (คล้าย Lag Cut) |
| **Click GUI** | - | UI แบบ WaterClient |

### วิธีเปิด Sus Chunk
1. เปิดเกม → กดปุ่มเปิด Click GUI (ปกติ **Right Shift**)
2. ไปหมวด **DONUT** → เปิด **SUS Chunk Finder**
3. ปรับ:
   - **Scan Radius** = ระยะสแกน
   - **Base Threshold** = จำนวนกล่องขั้นต่ำถึงจะถือว่าเป็นฐาน
   - **Fill Color / Fill Alpha** = สีและความทึบ
   - **Show % & Count** = แสดงเปอร์เซ็นต์

Chunk ที่เป็นฐานใต้ดินจะ **กระพริบสีขาว**  
Chunk ที่น่าสงสัยจาก amethyst จะถูกเติมสีตาม Fill Color

### Storage ESP
หมวด **RENDER** → **Storage ESP**  
เลือก Chest / Barrel / Shulker / Hopper / Spawner ได้

---

## วิธี Build

ต้องมี: JDK 21, Internet

```bash
cd WaterClient-src
chmod +x gradlew
./gradlew build
```

ไฟล์ jar อยู่ที่:
`build/libs/WaterClient-1.0.0.jar`

ใส่ใน `.minecraft/mods` พร้อม **Fabric Loader 1.21.11** + **Fabric API**

---

## รวมกับ Claude Client

WaterClient มีของที่ Claude มีอยู่แล้วเกือบครบ:
- Lag Cut → ใช้ **No Render**
- Player Debug → ใช้ **Player ESP**
- Storage ESP → มีอยู่แล้ว
- Sus Chunk → **SUS Chunk Finder**

ถ้าต้องการ **Auto Sell** จาก Claude เพิ่ม บอกได้ จะช่วยใส่เป็นโมดูลใน WaterClient ให้

---

## หมายเหตุ

- Client-side only
- ใช้บนเซิร์ฟที่อนุญาต client mod เท่านั้น
- โค้ดส่วนใหญ่มาจาก WaterClient v5 by catchingatom
