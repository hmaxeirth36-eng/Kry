# WaterClient v5 — Build (Fabric 1.21.11)

## Requirements
- **Java 21** (ไม่ใช่ 25)
- Internet สำหรับดาวน์โหลด Minecraft + dependencies

## Build
```bash
# ล้าง cache เก่าถ้าเคย fail
rm -rf .gradle build

# build
./gradlew build --no-daemon
```

Jar อยู่ที่: `build/libs/WaterClient-1.0.0.jar`

## ถ้า error เรื่อง Java 25 / Loom
โปรเจกต์นี้ใช้ **Loom 1.14.10** เท่านั้น (รองรับ Java 21)
อย่าเปลี่ยนเป็น Loom 1.18.x

## Features ที่แก้แล้ว
- Amethyst Sus Finder — ไม่กระพริบ %
- Chest Sus Finder — แยกโมดูล ตรวจกล่อง/redstone จาก Y
- Player Debug — แยกโมดูล ตรวจผู้เล่นใต้ดิน
- ตั้งปุ่ม Bind โมดูลได้แล้ว (กด Bind แล้วกดปุ่ม)

เปิด GUI: **Right Shift**
หมวด DONUT → เปิดโมดูลที่ต้องการ
