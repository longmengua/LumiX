/*
 * 檔案用途：領域工具，封裝簽名、JSON 處理或文字解析等純技術細節。
 */
package com.example.exchange.domain.util;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TeamNameParser
 *
 * 用途：
 * 解析比賽標題中的主客隊名稱，
 * 並統一不同資料來源的隊伍命名。
 *
 * 適用場景：
 * - Polymarket market title
 * - FIFA fixture title
 * - 第三方體育資料源
 *
 * 支援格式：
 * - Portugal vs Uzbekistan
 * - Portugal vs. Uzbekistan
 * - Portugal v Uzbekistan
 * - Portugal versus Uzbekistan
 *
 * 功能：
 * 1. parse team pair
 * 2. normalize team name
 * 3. alias mapping
 */
public final class TeamNameParser {

    /**
     * 隊伍名稱別名表。
     *
     * 不同來源可能對同一隊伍使用不同名稱，
     * 需要統一成系統內部標準名稱。
     */
    private static final Map<String, String> TEAM_ALIASES = Map.ofEntries(

            // 美國
            Map.entry("usa", "United States"),
            Map.entry("u.s.a", "United States"),
            Map.entry("us", "United States"),
            Map.entry("united states", "United States"),

            // 韓國
            Map.entry("south korea", "Korea Republic"),
            Map.entry("korea republic", "Korea Republic"),
            Map.entry("korea rep", "Korea Republic"),

            // 捷克
            Map.entry("czechia", "Czech Republic"),
            Map.entry("czech republic", "Czech Republic"),

            // 土耳其
            Map.entry("turkiye", "Türkiye"),
            Map.entry("türkiye", "Türkiye"),
            Map.entry("turkey", "Türkiye"),

            // 象牙海岸
            Map.entry("ivory coast", "Côte d'Ivoire"),
            Map.entry("cote d'ivoire", "Côte d'Ivoire")
    );

    /**
     * 支援的比賽分隔字。
     *
     * 支援：
     * - v
     * - vs
     * - vs.
     * - versus
     */
    private static final Pattern TEAM_SPLIT_PATTERN = Pattern.compile(
            "^(.*?)\\s+(?:v|vs\\.?|versus)\\s+(.*?)$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 工具類禁止實例化。
     */
    private TeamNameParser() {
    }

    /**
     * 解析主隊 / 客隊。
     *
     * 範例：
     * Portugal vs Uzbekistan
     * -> home = Portugal
     * -> away = Uzbekistan
     */
    public static TeamPair parseTeamPair(String title) {

        // 空值保護
        if (title == null || title.isBlank()) {
            return TeamPair.unknown();
        }

        /**
         * title normalize：
         * 1. 去除特殊空白
         * 2. 統一 dash
         * 3. 合併多空白
         */
        String normalized = title
                .replace('\u00A0', ' ')
                .replaceAll("[–—−]", "-")
                .replaceAll("\\s+", " ")
                .trim();

        Matcher matcher = TEAM_SPLIT_PATTERN.matcher(normalized);

        // 無法解析
        if (!matcher.matches()) {
            return new TeamPair(normalized, "UNKNOWN");
        }

        /**
         * group(1) = 主隊
         * group(2) = 客隊
         */
        String home = normalizeTeamName(matcher.group(1));
        String away = normalizeTeamName(matcher.group(2));

        return new TeamPair(home, away);
    }

    /**
     * 隊伍名稱 normalize。
     *
     * 用途：
     * 將不同來源的名稱轉成系統標準名稱。
     */
    public static String normalizeTeamName(String name) {

        // 空值保護
        if (name == null || name.isBlank()) {
            return "UNKNOWN";
        }

        /**
         * 基礎 normalize：
         * - 去除特殊空白
         * - 合併多空白
         * - 去除尾部標點
         */
        String normalized = name
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("[.,]+$", "")
                .trim();

        /**
         * alias lookup：
         * 若存在別名映射，
         * 則轉成標準名稱。
         */
        return TEAM_ALIASES.getOrDefault(
                normalized.toLowerCase(Locale.ROOT),
                normalized
        );
    }

    /**
     * 主客隊資料結構。
     */
    public record TeamPair(
            String homeTeam,
            String awayTeam
    ) {

        /**
         * UNKNOWN 預設值。
         */
        public static TeamPair unknown() {
            return new TeamPair("UNKNOWN", "UNKNOWN");
        }
    }
}