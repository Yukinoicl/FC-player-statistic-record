package com.fcdata.fcdataserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "career_total")
public class CareerTotal {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "latest_age", nullable = false)
    private Integer latestAge = 0;

    @Column(name = "continental_appearances", nullable = false)
    private Integer continentalAppearances = 0;

    @Column(name = "continental_goals", nullable = false)
    private Integer continentalGoals = 0;

    @Column(name = "continental_assists", nullable = false)
    private Integer continentalAssists = 0;

    @Column(name = "continental_titles", nullable = false)
    private Integer continentalTitles = 0;

    @Column(name = "continental_rating", nullable = false)
    private Double continentalRating = 0.0;

    @Column(name = "league_appearances", nullable = false)
    private Integer leagueAppearances = 0;

    @Column(name = "league_goals", nullable = false)
    private Integer leagueGoals = 0;

    @Column(name = "league_assists", nullable = false)
    private Integer leagueAssists = 0;

    @Column(name = "league_titles", nullable = false)
    private Integer leagueTitles = 0;

    @Column(name = "league_rating", nullable = false)
    private Double leagueRating = 0.0;

    @Column(name = "overall_appearances", nullable = false)
    private Integer overallAppearances = 0;

    @Column(name = "overall_goals", nullable = false)
    private Integer overallGoals = 0;

    @Column(name = "overall_assists", nullable = false)
    private Integer overallAssists = 0;

    @Column(name = "overall_league_titles", nullable = false)
    private Integer overallLeagueTitles = 0;

    @Column(name = "overall_continental_titles", nullable = false)
    private Integer overallContinentalTitles = 0;

    @Column(name = "overall_rating", nullable = false)
    private Double overallRating = 0.0;
}
