package com.water.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public final class RegionMapData {
   public static final int MAP_SIZE = 9;
   public static final double REGION_SIZE = 50000.0;
   public static final double MAP_OFFSET = 225000.0;
   public final Map<Integer, RegionInfo> regionMap = new HashMap<>();
   public final String[] typeNames = new String[]{"EU Central", "EU West", "NA East", "NA West", "Asia", "Oceania"};
   public final Color[] typeColors = new Color[]{
      new Color(159, 206, 99), new Color(0, 166, 99), new Color(79, 173, 234), new Color(47, 110, 186), new Color(245, 194, 66), new Color(252, 136, 3)
   };

   public RegionMapData() {
      int[][] aint = new int[][]{
         {82, 5},
         {100, 3},
         {101, 3},
         {102, 3},
         {103, 2},
         {104, 2},
         {105, 2},
         {106, 2},
         {91, 2},
         {83, 5},
         {44, 3},
         {75, 3},
         {42, 3},
         {41, 2},
         {40, 2},
         {39, 2},
         {38, 2},
         {92, 2},
         {84, 5},
         {45, 3},
         {14, 3},
         {13, 3},
         {12, 2},
         {11, 2},
         {10, 2},
         {37, 2},
         {93, 2},
         {85, 5},
         {46, 5},
         {74, 5},
         {3, 3},
         {2, 2},
         {1, 2},
         {25, 2},
         {36, 2},
         {94, 2},
         {86, 4},
         {47, 4},
         {72, 4},
         {71, 4},
         {5, 2},
         {4, 2},
         {24, 2},
         {35, 2},
         {95, 2},
         {87, 4},
         {51, 1},
         {17, 1},
         {9, 0},
         {8, 0},
         {7, 0},
         {23, 0},
         {34, 0},
         {96, 2},
         {88, 4},
         {54, 1},
         {18, 1},
         {61, 0},
         {62, 0},
         {21, 0},
         {22, 0},
         {33, 0},
         {97, 0},
         {89, 0},
         {26, 1},
         {27, 0},
         {28, 0},
         {29, 0},
         {30, 0},
         {59, 0},
         {32, 0},
         {98, 0},
         {90, 0},
         {107, 1},
         {108, 1},
         {109, 1},
         {110, 1},
         {111, 1},
         {112, 1},
         {113, 1},
         {99, 0}
      };

      for (int i = 0; i < aint.length; i++) {
         int j = aint[i][0];
         int k = Math.min(aint[i][1], this.typeNames.length - 1);
         this.regionMap.put(i, new RegionInfo(j, k, i / 9, i % 9));
      }
   }

   public RegionInfo getRegionInfo(int index) {
      return this.regionMap.get(index);
   }

   public int getMapSize() {
      return 9;
   }

   public String[] getRegionTypeNames() {
      return (String[])this.typeNames.clone();
   }

   public Color[] getRegionTypeColors() {
      return (Color[])this.typeColors.clone();
   }

   public Color getRegionColor(int typeIndex) {
      return typeIndex >= 0 && typeIndex < this.typeColors.length ? this.typeColors[typeIndex] : Color.GRAY;
   }

   public int getRegionAt(double x, double z) {
      int[] aint = this.worldToGrid(x, z);
      if (aint[0] >= 0 && aint[0] < 9 && aint[1] >= 0 && aint[1] < 9) {
         RegionInfo regionInfo = this.regionMap.get(aint[1] * 9 + aint[0]);
         return regionInfo != null ? regionInfo.regionId() : -1;
      } else {
         return -1;
      }
   }

   public String getRegionTypeName(double x, double z) {
      int[] aint = this.worldToGrid(x, z);
      if (aint[0] >= 0 && aint[0] < 9 && aint[1] >= 0 && aint[1] < 9) {
         RegionInfo regionInfo = this.regionMap.get(aint[1] * 9 + aint[0]);
         return regionInfo != null && regionInfo.regionType() >= 0 && regionInfo.regionType() < this.typeNames.length
            ? this.typeNames[regionInfo.regionType()]
            : "Unknown";
      } else {
         return "Unknown";
      }
   }

   public int[] worldToGrid(double x, double z) {
      return new int[]{(int)((x + 225000.0) / 50000.0), (int)((z + 225000.0) / 50000.0)};
   }

   public double[] worldToCellPosition(double x, double z) {
      double d0 = (x + 225000.0) % 50000.0 / 50000.0;
      double d1 = (z + 225000.0) % 50000.0 / 50000.0;
      return new double[]{Math.max(0.0, Math.min(1.0, d0)), Math.max(0.0, Math.min(1.0, d1))};
   }
}
