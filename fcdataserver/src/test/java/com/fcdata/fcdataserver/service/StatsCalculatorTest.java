package com.fcdata.fcdataserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fcdata.fcdataserver.entity.CareerTotal;
import com.fcdata.fcdataserver.entity.SeasonRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatsCalculatorTest {

    @Test
    void overallRatingIsWeightedByAppearances() {
        SeasonRecord first = season("第1赛季", 20, 10, 8.00, 40, 7.00, 1);
        SeasonRecord second = season("第2赛季", 21, 5, 9.00, 10, 8.00, 2);

        CareerTotal total = new CareerTotal();
        StatsCalculator.applyTotals(total, List.of(first, second));

        assertEquals(15, total.getContinentalAppearances());
        assertEquals(50, total.getLeagueAppearances());
        assertEquals(65, total.getOverallAppearances());
        assertEquals(21, total.getLatestAge());
        assertEquals(2, total.getOverallLeagueTitles());
        assertEquals(0, total.getOverallContinentalTitles());
        assertEquals(8.33, total.getContinentalRating());
        assertEquals(7.20, total.getLeagueRating());
        assertEquals(7.46, total.getOverallRating());
    }

    @Test
    void nextSeasonNameIncrementsYearPair() {
        SeasonRecord season = new SeasonRecord();
        season.setSeasonName("2026/27");
        season.setSortOrder(1);
        season.setId(1L);
        assertEquals("2027/28", StatsCalculator.nextSeasonName(List.of(season)));
    }

    private SeasonRecord season(
            String name,
            int age,
            int continentalApps,
            double continentalRating,
            int leagueApps,
            double leagueRating,
            int sortOrder
    ) {
        SeasonRecord record = new SeasonRecord();
        record.setId((long) sortOrder);
        record.setSeasonName(name);
        record.setAge(age);
        record.setContinentalAppearances(continentalApps);
        record.setContinentalRating(continentalRating);
        record.setLeagueAppearances(leagueApps);
        record.setLeagueRating(leagueRating);
        record.setLeagueTitles(1);
        record.setSortOrder(sortOrder);
        return record;
    }
}
