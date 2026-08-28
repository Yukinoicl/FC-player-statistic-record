package com.fcdata.fcdataserver.dto;

import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record PlayerSummaryDto(Long id, String name) {
    }

    public record CreatePlayerRequest(String name) {
    }

    public record RenamePlayerRequest(String name) {
    }

    public record ReorderPlayersRequest(List<Long> ids) {
    }

    public record StatLineDto(
            int appearances,
            int goals,
            int assists,
            int titles,
            double rating
    ) {
    }

    public record OverallStatDto(
            int appearances,
            int goals,
            int assists,
            int leagueTitles,
            int continentalTitles,
            double rating
    ) {
    }

    public record SeasonRecordDto(
            Long id,
            String seasonName,
            int age,
            StatLineDto continental,
            StatLineDto league
    ) {
    }

    public record CareerTotalsDto(
            int latestAge,
            StatLineDto continental,
            StatLineDto league,
            OverallStatDto overall
    ) {
    }

    public record PlayerDetailDto(
            Long id,
            String name,
            List<SeasonRecordDto> seasons,
            CareerTotalsDto totals
    ) {
    }

    public record SeasonUpsertRequest(
            String seasonName,
            Integer age,
            Integer continentalAppearances,
            Integer continentalGoals,
            Integer continentalAssists,
            Integer continentalTitles,
            Double continentalRating,
            Integer leagueAppearances,
            Integer leagueGoals,
            Integer leagueAssists,
            Integer leagueTitles,
            Double leagueRating
    ) {
    }
}
