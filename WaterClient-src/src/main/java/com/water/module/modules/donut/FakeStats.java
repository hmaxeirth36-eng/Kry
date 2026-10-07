package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.util.FakeStatsData;
import com.water.util.ScoreLine;
import com.water.util.StyledValue;
import com.water.util.TabListData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.ScoreboardCriterion.RenderType;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class FakeStats extends Module {
   public static FakeStats INSTANCE;
   public static final String OBJECTIVE_NAME = "water_fake_stats";
   public static final int MAX_SIDEBAR_LINES = 15;
   public final Setting<String> money = new Setting<>("Money", "0");
   public final Setting<String> shards = new Setting<>("Shards", "0");
   public final Setting<String> kills = new Setting<>("Kills", "0");
   public final Setting<String> deaths = new Setting<>("Deaths", "0");
   public final Setting<String> playtime = new Setting<>("Playtime", "0m");
   public final Random randomSource = new Random();
   public ScoreboardObjective originalObjective;
   public String originalObjectiveName;
   public ScoreboardObjective customObjective;
   public FakeStatsData appliedStats;
   public Object lastWorld;
   public String lastSnapshotSignature = "";
   public boolean needsRefresh;
   public long lastRebuildMs = 0L;
   public static final long REBUILD_COOLDOWN_MS = 500L;

   public static FakeStats getInstance() {
      return INSTANCE;
   }

   public FakeStats() {
      super("FakeStats", Category.DONUT);
      INSTANCE = this;
      this.addSetting(this.money);
      this.addSetting(this.shards);
      this.addSetting(this.kills);
      this.addSetting(this.deaths);
      this.addSetting(this.playtime);
   }

   @Override
   public void onEnable() {
      this.lastWorld = mc.world;
      this.originalObjective = null;
      this.originalObjectiveName = null;
      this.customObjective = null;
      this.lastSnapshotSignature = "";
      this.randomizeStats();
      this.applyStats();
      this.needsRefresh = true;
   }

   @Override
   public void onDisable() {
      this.lastWorld = null;
      this.lastSnapshotSignature = "";
      this.needsRefresh = false;
      ScoreboardObjective scoreboardObjective = this.originalObjective;
      ScoreboardObjective scoreboardObjective1 = this.customObjective;
      this.originalObjective = null;
      this.originalObjectiveName = null;
      this.customObjective = null;
      if (mc.world != null) {
         Scoreboard scoreboard = mc.world.getScoreboard();

         try {
            if (scoreboardObjective1 != null) {
               scoreboard.removeObjective(scoreboardObjective1);
            }

            if (scoreboardObjective != null && scoreboard.getObjectives().contains(scoreboardObjective)) {
               scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, scoreboardObjective);
            }
         } catch (Exception exception) {
         }
      }
   }

   @Override
   public void onTick() {
      if (mc.world == null) {
         this.originalObjective = null;
         this.originalObjectiveName = null;
         this.customObjective = null;
         this.lastWorld = null;
         this.lastSnapshotSignature = "";
      } else {
         if (mc.world != this.lastWorld) {
            this.originalObjective = null;
            this.originalObjectiveName = null;
            this.customObjective = null;
            this.lastSnapshotSignature = "";
            this.lastWorld = mc.world;
            this.needsRefresh = true;
         }

         this.ensureFakeObjective();
         if (this.originalObjective != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            if (!scoreboard.getObjectives().contains(this.originalObjective)) {
               this.originalObjective = null;
               this.ensureFakeObjective();
               if (this.originalObjective == null) {
                  return;
               }
            }

            TabListData tablistdata = this.readSidebar(scoreboard, this.originalObjective);
            if (tablistdata != null) {
               this.refreshStatsIfChanged();
               if (!tablistdata.signature().equals(this.lastSnapshotSignature) || this.customObjective == null) {
                  this.needsRefresh = true;
               }

               this.rebuildSidebar(scoreboard, tablistdata);
               if (this.customObjective != null && scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != this.customObjective) {
                  scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, this.customObjective);
               }
            }
         }
      }
   }

   public void randomizeStats() {
      this.money.setValue(this.formatCompact(this.randomBetween(10000L, 5000000000L)));
      this.shards.setValue(this.formatCompact(this.randomBetween(0L, 2500000L)));
      this.kills.setValue(String.valueOf(this.randomBetween(0L, 2000L)));
      this.deaths.setValue(String.valueOf(this.randomBetween(0L, 1000L)));
      this.playtime.setValue(this.formatPlaytime(this.randomBetween(0L, 15552000L)));
   }

   public void applyStats() {
      this.appliedStats = new FakeStatsData(
         this.orDefault(this.money.getValue(), "0"),
         this.orDefault(this.shards.getValue(), "0"),
         this.orDefault(this.kills.getValue(), "0"),
         this.orDefault(this.deaths.getValue(), "0"),
         this.orDefault(this.playtime.getValue(), "0m")
      );
      this.needsRefresh = true;
   }

   public void refreshStatsIfChanged() {
      FakeStatsData fakeStatsData = new FakeStatsData(
         this.orDefault(this.money.getValue(), "0"),
         this.orDefault(this.shards.getValue(), "0"),
         this.orDefault(this.kills.getValue(), "0"),
         this.orDefault(this.deaths.getValue(), "0"),
         this.orDefault(this.playtime.getValue(), "0m")
      );
      if (this.appliedStats == null || !this.appliedStats.lQ().equals(fakeStatsData.lQ())) {
         this.appliedStats = fakeStatsData;
         this.needsRefresh = true;
      }
   }

   public void ensureFakeObjective() {
      if (mc.world != null) {
         Scoreboard scoreboard = mc.world.getScoreboard();
         ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (scoreboardObjective != null && !"water_fake_stats".equals(scoreboardObjective.getName())) {
            this.originalObjective = scoreboardObjective;
            this.originalObjectiveName = scoreboardObjective.getName();
         } else if (this.originalObjective == null || !scoreboard.getObjectives().contains(this.originalObjective)) {
            if (this.originalObjectiveName != null) {
               ScoreboardObjective scoreboardObjective1 = scoreboard.getNullableObjective(this.originalObjectiveName);
               if (scoreboardObjective1 != null && !"water_fake_stats".equals(scoreboardObjective1.getName())) {
                  this.originalObjective = scoreboardObjective1;
                  return;
               }
            }

            for (ScoreboardObjective scoreboardObjective2 : scoreboard.getObjectives()) {
               if (!"water_fake_stats".equals(scoreboardObjective2.getName())) {
                  this.originalObjective = scoreboardObjective2;
                  this.originalObjectiveName = scoreboardObjective2.getName();
                  return;
               }
            }
         }
      }
   }

   public TabListData readSidebar(Scoreboard scoreboard, ScoreboardObjective objective) {
      ArrayList arrayList = new ArrayList();
      ArrayList<ScoreboardEntry> arrayList1 = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
      arrayList1.removeIf(ScoreboardEntry::hidden);
      arrayList1.sort(Comparator.comparingInt(ScoreboardEntry::value).reversed());
      if (arrayList1.size() > 15) {
         arrayList1 = new ArrayList<>(arrayList1.subList(0, 15));
      }

      for (ScoreboardEntry scoreboardEntry : (Iterable<ScoreboardEntry>)arrayList1) {
         arrayList.add(new ScoreLine(scoreboardEntry.value(), this.getEntryText(scoreboard, scoreboardEntry)));
      }

      MutableText mutableText = objective.getDisplayName() != null ? objective.getDisplayName().copy() : Text.literal("Donut SMP");
      StringBuilder stringBuilder = new StringBuilder(mutableText.getString());

      for (ScoreLine scoreLine : (Iterable<ScoreLine>)arrayList) {
         stringBuilder.append('\n').append(scoreLine.score()).append(':').append(scoreLine.text().getString());
      }

      stringBuilder.append('\n').append(this.appliedStats != null ? this.appliedStats.lQ() : "");
      return new TabListData(mutableText, arrayList, stringBuilder.toString());
   }

   public Text getEntryText(Scoreboard scoreboard, ScoreboardEntry entry) {
      if (entry.display() != null) {
         return entry.display().copy();
      } else {
         MutableText mutableText = entry.name() != null ? entry.name().copy() : Text.literal(entry.owner());
         Team team = scoreboard.getScoreHolderTeam(entry.owner());
         return Team.decorateName(team, mutableText).copy();
      }
   }

   public void rebuildSidebar(Scoreboard scoreboard, TabListData tabListData) {
      if (this.needsRefresh && this.appliedStats != null) {
         long i = System.currentTimeMillis();
         if (i - this.lastRebuildMs >= 500L) {
            this.lastRebuildMs = i;
            ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective("water_fake_stats");
            if (scoreboardObjective != null) {
               scoreboard.removeObjective(scoreboardObjective);
            }

            this.customObjective = scoreboard.addObjective(
               "water_fake_stats", ScoreboardCriterion.DUMMY, tabListData.title().copy(), RenderType.INTEGER, true, BlankNumberFormat.INSTANCE
            );
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, this.customObjective);
            List list = tabListData.lines();
            int j = 0;

            for (int k = 0; k < list.size(); k++) {
               ScoreLine scoreLine = (ScoreLine)list.get(k);
               ScoreHolder scoreHolder = ScoreHolder.fromName("fake_stats_line_" + k);
               ScoreAccess scoreAccess = scoreboard.getOrCreateScore(scoreHolder, this.customObjective);
               scoreAccess.setScore(list.size() - k);
               String s = scoreLine.text().getString().trim();
               boolean flag = s.matches(".*\\d.*");
               if (flag) {
                  scoreAccess.setDisplayText(this.replaceStatValue(scoreLine.text(), j));
                  j++;
               } else {
                  scoreAccess.setDisplayText(scoreLine.text().copy());
               }

               scoreAccess.setNumberFormat(BlankNumberFormat.INSTANCE);
            }

            this.lastSnapshotSignature = tabListData.signature();
            this.needsRefresh = false;
         }
      }
   }

   public Text replaceStatValue(Text text, int index) {
      String[] astring = new String[]{
         this.appliedStats.money(), this.appliedStats.shards(), this.appliedStats.kills(), this.appliedStats.deaths(), this.appliedStats.playtime()
      };
      if (index >= astring.length) {
         return text.copy();
      } else {
         String s = astring[index];
         List list = this.flattenStyled(text);
         String s1 = text.getString();
         int i = -1;

         for (int j = 0; j < s1.length(); j++) {
            char c0 = s1.charAt(j);
            if (Character.isDigit(c0) || c0 == '-' && j + 1 < s1.length() && Character.isDigit(s1.charAt(j + 1))) {
               i = j;
               break;
            }
         }

         if (i >= 0) {
            MutableText mutableText1 = Text.empty();
            this.appendStyledPrefix(mutableText1, list, i);
            mutableText1.append(Text.literal(s).setStyle(this.styleAtIndex(list, i)));
            return mutableText1;
         } else {
            MutableText mutableText = Text.empty();

            for (StyledValue styledValue : (Iterable<StyledValue>)list) {
               mutableText.append(Text.literal(styledValue.value()).setStyle(styledValue.style()));
            }

            return mutableText;
         }
      }
   }

   public List<StyledValue> flattenStyled(Text text) {
      ArrayList arrayList = new ArrayList();
      text.visit((var1x, var2x) -> {
         if (!var2x.isEmpty()) {
            arrayList.add(new StyledValue(var2x, var1x));
         }

         return Optional.empty();
      }, Style.EMPTY);
      return arrayList;
   }

   public void appendStyledPrefix(MutableText text, List<StyledValue> list, int length) {
      int i = Math.max(0, length);

      for (StyledValue styledValue : list) {
         if (i <= 0) {
            return;
         }

         String s = styledValue.value();
         int j = Math.min(s.length(), i);
         text.append(Text.literal(s.substring(0, j)).setStyle(styledValue.style()));
         i -= j;
      }
   }

   public Style styleAtIndex(List<StyledValue> list, int index) {
      int i = Math.max(0, index);
      Style style = Style.EMPTY;

      for (StyledValue styledValue : list) {
         if (!styledValue.value().isEmpty()) {
            style = styledValue.style();
         }

         if (i < styledValue.value().length()) {
            return styledValue.style();
         }

         i -= styledValue.value().length();
      }

      return style;
   }

   public void removeFakeObjective() {
      if (mc.world != null) {
         Scoreboard scoreboard = mc.world.getScoreboard();
         ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective("water_fake_stats");
         if (scoreboardObjective != null) {
            scoreboard.removeObjective(scoreboardObjective);
         }

         if (this.originalObjective != null && scoreboard.getObjectives().contains(this.originalObjective)) {
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, this.originalObjective);
         }
      }
   }

   public long randomBetween(long min, long max) {
      return min >= max ? min : min + (long)Math.floor(this.randomSource.nextDouble() * (max - min + 1L));
   }

   public String formatCompact(long var1) {
      long i = Math.abs(var1);
      if (i < 1000L) {
         return Long.toString(var1);
      } else if (i < 1000000L) {
         return this.formatWithSuffix(var1 / 1000.0, "K");
      } else {
         return i < 1000000000L ? this.formatWithSuffix(var1 / 1000000.0, "M") : this.formatWithSuffix(var1 / 1.0E9, "B");
      }
   }

   public String formatWithSuffix(double var1, String text) {
      String s = var1 >= 100.0 ? "%.0f%s" : (var1 >= 10.0 ? "%.1f%s" : "%.2f%s");
      return String.format(Locale.US, s, var1, text);
   }

   public String formatPlaytime(long var1) {
      long i = var1 / 3600L;
      long j = i / 24L;
      long k = i % 24L;
      long l = var1 % 3600L / 60L;
      if (j > 0L) {
         return String.format(Locale.US, "%dd %dh", j, k);
      } else {
         return i > 0L ? String.format(Locale.US, "%dh %dm", i, l) : String.format(Locale.US, "%dm", l);
      }
   }

   public String orDefault(String text, String text2) {
      if (text == null) {
         return text2;
      } else {
         String s = text.trim();
         return s.isEmpty() ? text2 : s;
      }
   }

   public Text fakeFooterText(Text text) {
      if (this.appliedStats == null) {
         return text;
      } else {
         String s = text.getString();
         Pattern pattern = Pattern.compile("(\\$\\s*)([0-9][0-9.,]*[KkMmBbTt]?)");
         Matcher matcher = pattern.matcher(s);
         if (!matcher.find()) {
            return text;
         } else {
            String s1 = s.substring(0, matcher.start(2)) + this.appliedStats.money() + s.substring(matcher.end(2));
            List list = this.flattenStyled(text);
            Style style = list.isEmpty() ? Style.EMPTY : ((StyledValue)list.get(0)).style();
            return Text.literal(s1).setStyle(style);
         }
      }
   }
}
