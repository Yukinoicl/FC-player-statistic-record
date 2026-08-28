package com.fcdata.fcdataserver.service;

import com.fcdata.fcdataserver.entity.CareerTotal;
import com.fcdata.fcdataserver.entity.SeasonRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StatsCalculator {

    private static final Pattern YEAR_SEASON = Pattern.compile("^(\\d{4})/(\\d{2})$");
    private static final Pattern INDEX_SEASON = Pattern.compile("^第(\\d+)赛季$");

    private StatsCalculator() {
    }

    public static void applyTotals(CareerTotal total, List<SeasonRecord> seasons) {
        int continentalApps = 0;
        int continentalGoals = 0;
        int continentalAssists = 0;
        int continentalTitles = 0;
        BigDecimal continentalWeighted = BigDecimal.ZERO;

        int leagueApps = 0;
        int leagueGoals = 0;
        int leagueAssists = 0;
        int leagueTitles = 0;
        BigDecimal leagueWeighted = BigDecimal.ZERO;

        for (SeasonRecord season : seasons) {
            continentalApps += n(season.getContinentalAppearances());
            continentalGoals += n(season.getContinentalGoals());
            continentalAssists += n(season.getContinentalAssists());
            continentalTitles += n(season.getContinentalTitles());
            continentalWeighted = continentalWeighted.add(weighted(season.getContinentalRating(), season.getContinentalAppearances()));

            leagueApps += n(season.getLeagueAppearances());
            leagueGoals += n(season.getLeagueGoals());
            leagueAssists += n(season.getLeagueAssists());
            leagueTitles += n(season.getLeagueTitles());
            leagueWeighted = leagueWeighted.add(weighted(season.getLeagueRating(), season.getLeagueAppearances()));
        }

        int overallApps = continentalApps + leagueApps;
        BigDecimal overallWeighted = continentalWeighted.add(leagueWeighted);

        total.setLatestAge(latestAge(seasons));
        total.setContinentalAppearances(continentalApps);
        total.setContinentalGoals(continentalGoals);
        total.setContinentalAssists(continentalAssists);
        total.setContinentalTitles(continentalTitles);
        total.setContinentalRating(average(continentalWeighted, continentalApps));
        total.setLeagueAppearances(leagueApps);
        total.setLeagueGoals(leagueGoals);
        total.setLeagueAssists(leagueAssists);
        total.setLeagueTitles(leagueTitles);
        total.setLeagueRating(average(leagueWeighted, leagueApps));
        total.setOverallAppearances(overallApps);
        total.setOverallGoals(continentalGoals + leagueGoals);
        total.setOverallAssists(continentalAssists + leagueAssists);
        total.setOverallLeagueTitles(leagueTitles);
        total.setOverallContinentalTitles(continentalTitles);
        total.setOverallRating(average(overallWeighted, overallApps));
    }

    public static String nextSeasonName(List<SeasonRecord> seasons) {
        if (seasons.isEmpty()) {
            return "第1赛季";
        }
        SeasonRecord last = lastSeason(seasons);
        Matcher year = YEAR_SEASON.matcher(last.getSeasonName() == null ? "" : last.getSeasonName());
        if (year.matches()) {
            int start = Integer.parseInt(year.group(1)) + 1;
            int end = (start + 1) % 100;
            return start + "/" + String.format("%02d", end);
        }
        Matcher index = INDEX_SEASON.matcher(last.getSeasonName() == null ? "" : last.getSeasonName());
        if (index.matches()) {
            return "第" + (Integer.parseInt(index.group(1)) + 1) + "赛季";
        }
        return "第" + (seasons.size() + 1) + "赛季";
    }

    public static int nextAge(List<SeasonRecord> seasons) {
        if (seasons.isEmpty()) {
            return 18;
        }
        return Math.max(0, n(lastSeason(seasons).getAge()) + 1);
    }

    public static int nextSortOrder(List<SeasonRecord> seasons) {
        return seasons.stream()
                .map(SeasonRecord::getSortOrder)
                .mapToInt(StatsCalculator::n)
                .max()
                .orElse(0) + 1;
    }

    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static SeasonRecord lastSeason(List<SeasonRecord> seasons) {
        return seasons.stream()
                .max(Comparator.comparing((SeasonRecord s) -> n(s.getSortOrder()))
                        .thenComparing(s -> s.getId() == null ? 0L : s.getId()))
                .orElse(seasons.get(seasons.size() - 1));
    }

    private static int latestAge(List<SeasonRecord> seasons) {
        if (seasons.isEmpty()) {
            return 0;
        }
        return n(lastSeason(seasons).getAge());
    }

    private static BigDecimal weighted(Double rating, Integer appearances) {
        int apps = n(appearances);
        if (apps <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(rating == null ? 0 : rating).multiply(BigDecimal.valueOf(apps));
    }

    private static double average(BigDecimal weightedSum, int appearances) {
        if (appearances <= 0) {
            return 0;
        }
        return weightedSum
                .divide(BigDecimal.valueOf(appearances), 8, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static int n(Integer value) {
        return value == null ? 0 : value;
    }
}
