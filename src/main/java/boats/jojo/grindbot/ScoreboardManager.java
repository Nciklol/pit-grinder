package boats.jojo.grindbot;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScoreboardManager {

    public static final Pattern SIDEBAR_EMOJI_PATTERN = Pattern.compile(
            "[\uD83D\uDD2B\uD83C\uDF6B\uD83D\uDCA3\uD83D\uDC7D\uD83D\uDD2E\uD83D\uDC0D\uD83D\uDC7E\uD83C\uDF20\uD83C\uDF6D\u26BD\uD83C\uDFC0\uD83D\uDC79\uD83C\uDF81\uD83C\uDF89\uD83C\uDF82]+");

    public static String scoreboardTitle;
    public static String strippedScoreboardTitle;

    public static List<String> scoreboardLines;
    public static List<String> strippedScoreboardLines;

    public static long lastFoundScoreboard = -1;
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-ORZ]");

    public static void processScoreboard(Scoreboard scoreboard) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.isSingleplayer()) {
            clear();
            return;
        }

        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null) {
            clear();
            return;
        }

        lastFoundScoreboard = System.currentTimeMillis();

        // Update titles
        scoreboardTitle = sidebarObjective.getDisplayName();
        strippedScoreboardTitle = STRIP_COLOR_PATTERN.matcher(scoreboardTitle).replaceAll("");

        // Update score lines
        Collection<Score> scores = scoreboard.getSortedScores(sidebarObjective);
        List<Score> filteredScores = scores.stream()
                .filter(p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());
        if (filteredScores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(filteredScores, scores.size() - 15));
        } else {
            scores = filteredScores;
        }

        Collections.reverse(filteredScores);

        scoreboardLines = new ArrayList<>();
        strippedScoreboardLines = new ArrayList<>();

        for (Score line : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(line.getPlayerName());
            String scoreboardLine = ScorePlayerTeam.formatPlayerName(team, line.getPlayerName()).trim();
            String cleansedScoreboardLine = SIDEBAR_EMOJI_PATTERN.matcher(scoreboardLine).replaceAll("");
            String colorStrippedCleansedScoreboardLine = STRIP_COLOR_PATTERN.matcher(scoreboardLine).replaceAll("");
            String emojiStrippedCleansedScoreboardLine = SIDEBAR_EMOJI_PATTERN
                    .matcher(colorStrippedCleansedScoreboardLine).replaceAll("");
            scoreboardLines.add(cleansedScoreboardLine);
            strippedScoreboardLines.add(emojiStrippedCleansedScoreboardLine);
        }
    }

    private static void clear() {
        scoreboardTitle = strippedScoreboardTitle = null;
        scoreboardLines = strippedScoreboardLines = null;
    }

    public static boolean hasScoreboard() {
        return scoreboardTitle != null;
    }

    public static int getNumberOfLines() {
        return scoreboardLines.size();
    }
}