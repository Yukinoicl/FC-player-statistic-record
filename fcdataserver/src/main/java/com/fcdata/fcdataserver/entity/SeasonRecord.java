package com.fcdata.fcdataserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "season_record")
public class SeasonRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "season_name", nullable = false, length = 40)
    private String seasonName;

    @Column(nullable = false)
    private Integer age = 0;

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

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
