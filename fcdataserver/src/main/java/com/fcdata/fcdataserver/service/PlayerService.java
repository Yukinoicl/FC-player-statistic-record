package com.fcdata.fcdataserver.service;

import com.fcdata.fcdataserver.dto.ApiDtos.CareerTotalsDto;
import com.fcdata.fcdataserver.dto.ApiDtos.CreatePlayerRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.OverallStatDto;
import com.fcdata.fcdataserver.dto.ApiDtos.PlayerDetailDto;
import com.fcdata.fcdataserver.dto.ApiDtos.PlayerSummaryDto;
import com.fcdata.fcdataserver.dto.ApiDtos.RenamePlayerRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.ReorderPlayersRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.SeasonRecordDto;
import com.fcdata.fcdataserver.dto.ApiDtos.SeasonUpsertRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.StatLineDto;
import com.fcdata.fcdataserver.entity.CareerTotal;
import com.fcdata.fcdataserver.entity.Player;
import com.fcdata.fcdataserver.entity.SeasonRecord;
import com.fcdata.fcdataserver.repository.CareerTotalRepository;
import com.fcdata.fcdataserver.repository.PlayerRepository;
import com.fcdata.fcdataserver.repository.SeasonRecordRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final SeasonRecordRepository seasonRecordRepository;
    private final CareerTotalRepository careerTotalRepository;

    public PlayerService(
            PlayerRepository playerRepository,
            SeasonRecordRepository seasonRecordRepository,
            CareerTotalRepository careerTotalRepository
    ) {
        this.playerRepository = playerRepository;
        this.seasonRecordRepository = seasonRecordRepository;
        this.careerTotalRepository = careerTotalRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerSummaryDto> listPlayers() {
        return playerRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(player -> new PlayerSummaryDto(player.getId(), player.getName()))
                .toList();
    }

    @Transactional
    public PlayerDetailDto createPlayer(CreatePlayerRequest request) {
        String name = requireName(request == null ? null : request.name());
        Player player = new Player();
        player.setName(name);
        player.setSortOrder(nextPlayerOrder());
        player = playerRepository.save(player);

        CareerTotal total = new CareerTotal();
        total.setPlayerId(player.getId());
        StatsCalculator.applyTotals(total, List.of());
        careerTotalRepository.save(total);
        return toDetail(player, List.of(), total);
    }

    @Transactional
    public PlayerSummaryDto renamePlayer(Long playerId, RenamePlayerRequest request) {
        Player player = requirePlayer(playerId);
        player.setName(requireName(request == null ? null : request.name()));
        playerRepository.save(player);
        return new PlayerSummaryDto(player.getId(), player.getName());
    }

    @Transactional
    public List<PlayerSummaryDto> reorderPlayers(ReorderPlayersRequest request) {
        List<Long> ids = request == null ? List.of() : request.ids();
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("排序列表不能为空");
        }
        if (ids.size() != new HashSet<>(ids).size()) {
            throw new IllegalArgumentException("排序列表有重复球员");
        }
        List<Player> players = playerRepository.findAll();
        if (ids.size() != players.size()) {
            throw new IllegalArgumentException("排序列表与球员数量不一致");
        }
        Map<Long, Player> byId = players.stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        int order = 1;
        for (Long id : ids) {
            Player player = byId.get(id);
            if (player == null) {
                throw new NoSuchElementException("球员不存在");
            }
            player.setSortOrder(order++);
        }
        playerRepository.saveAll(players);
        return listPlayers();
    }

    @Transactional
    public void deletePlayer(Long playerId) {
        requirePlayer(playerId);
        seasonRecordRepository.deleteByPlayerId(playerId);
        careerTotalRepository.deleteById(playerId);
        playerRepository.deleteById(playerId);
    }

    @Transactional
    public PlayerDetailDto getPlayer(Long playerId) {
        Player player = requirePlayer(playerId);
        List<SeasonRecord> seasons = seasonRecordRepository.findByPlayerIdOrderBySortOrderAscIdAsc(playerId);
        CareerTotal total = refreshTotals(playerId, seasons);
        return toDetail(player, seasons, total);
    }

    @Transactional
    public PlayerDetailDto addSeason(Long playerId, SeasonUpsertRequest request) {
        Player player = requirePlayer(playerId);
        List<SeasonRecord> seasons = seasonRecordRepository.findByPlayerIdOrderBySortOrderAscIdAsc(playerId);

        SeasonRecord record = new SeasonRecord();
        record.setPlayerId(playerId);
        record.setSortOrder(StatsCalculator.nextSortOrder(seasons));
        applySeasonDefaults(record, seasons, request);
        seasonRecordRepository.save(record);

        List<SeasonRecord> updated = seasonRecordRepository.findByPlayerIdOrderBySortOrderAscIdAsc(playerId);
        CareerTotal total = refreshTotals(playerId, updated);
        return toDetail(player, updated, total);
    }

    @Transactional
    public PlayerDetailDto updateSeason(Long playerId, Long seasonId, SeasonUpsertRequest request) {
        Player player = requirePlayer(playerId);
        SeasonRecord record = seasonRecordRepository.findByIdAndPlayerId(seasonId, playerId)
                .orElseThrow(() -> new NoSuchElementException("赛季不存在"));
        applySeasonValues(record, request, false);
        seasonRecordRepository.save(record);

        List<SeasonRecord> seasons = seasonRecordRepository.findByPlayerIdOrderBySortOrderAscIdAsc(playerId);
        CareerTotal total = refreshTotals(playerId, seasons);
        return toDetail(player, seasons, total);
    }

    @Transactional
    public PlayerDetailDto deleteSeason(Long playerId, Long seasonId) {
        Player player = requirePlayer(playerId);
        SeasonRecord record = seasonRecordRepository.findByIdAndPlayerId(seasonId, playerId)
                .orElseThrow(() -> new NoSuchElementException("赛季不存在"));
        seasonRecordRepository.delete(record);

        List<SeasonRecord> seasons = seasonRecordRepository.findByPlayerIdOrderBySortOrderAscIdAsc(playerId);
        CareerTotal total = refreshTotals(playerId, seasons);
        return toDetail(player, seasons, total);
    }

    private CareerTotal refreshTotals(Long playerId, List<SeasonRecord> seasons) {
        CareerTotal total = careerTotalRepository.findById(playerId).orElseGet(() -> {
            CareerTotal created = new CareerTotal();
            created.setPlayerId(playerId);
            return created;
        });
        StatsCalculator.applyTotals(total, seasons);
        return careerTotalRepository.save(total);
    }

    private void applySeasonDefaults(SeasonRecord record, List<SeasonRecord> existing, SeasonUpsertRequest request) {
        if (request == null) {
            record.setSeasonName(StatsCalculator.nextSeasonName(existing));
            record.setAge(StatsCalculator.nextAge(existing));
            return;
        }
        String seasonName = blankToNull(request.seasonName());
        record.setSeasonName(seasonName == null ? StatsCalculator.nextSeasonName(existing) : seasonName);
        record.setAge(request.age() == null ? StatsCalculator.nextAge(existing) : sanitizeAge(request.age()));
        applySeasonValues(record, request, true);
    }

    private void applySeasonValues(SeasonRecord record, SeasonUpsertRequest request, boolean skipNulls) {
        if (request == null) {
            return;
        }
        if (!skipNulls || request.seasonName() != null) {
            String seasonName = requireText(request.seasonName(), "赛季名称不能为空", 40);
            record.setSeasonName(seasonName);
        }
        if (!skipNulls || request.age() != null) {
            record.setAge(sanitizeAge(request.age()));
        }
        record.setContinentalAppearances(sanitizeCount(request.continentalAppearances(), skipNulls, record.getContinentalAppearances()));
        record.setContinentalGoals(sanitizeCount(request.continentalGoals(), skipNulls, record.getContinentalGoals()));
        record.setContinentalAssists(sanitizeCount(request.continentalAssists(), skipNulls, record.getContinentalAssists()));
        record.setContinentalTitles(sanitizeCount(request.continentalTitles(), skipNulls, record.getContinentalTitles()));
        record.setContinentalRating(sanitizeRating(request.continentalRating(), skipNulls, record.getContinentalRating()));
        record.setLeagueAppearances(sanitizeCount(request.leagueAppearances(), skipNulls, record.getLeagueAppearances()));
        record.setLeagueGoals(sanitizeCount(request.leagueGoals(), skipNulls, record.getLeagueGoals()));
        record.setLeagueAssists(sanitizeCount(request.leagueAssists(), skipNulls, record.getLeagueAssists()));
        record.setLeagueTitles(sanitizeCount(request.leagueTitles(), skipNulls, record.getLeagueTitles()));
        record.setLeagueRating(sanitizeRating(request.leagueRating(), skipNulls, record.getLeagueRating()));
    }

    private PlayerDetailDto toDetail(Player player, List<SeasonRecord> seasons, CareerTotal total) {
        List<SeasonRecordDto> seasonDtos = seasons.stream().map(this::toSeasonDto).toList();
        CareerTotalsDto totals = new CareerTotalsDto(
                n(total.getLatestAge()),
                new StatLineDto(
                        n(total.getContinentalAppearances()),
                        n(total.getContinentalGoals()),
                        n(total.getContinentalAssists()),
                        n(total.getContinentalTitles()),
                        StatsCalculator.round2(d(total.getContinentalRating()))
                ),
                new StatLineDto(
                        n(total.getLeagueAppearances()),
                        n(total.getLeagueGoals()),
                        n(total.getLeagueAssists()),
                        n(total.getLeagueTitles()),
                        StatsCalculator.round2(d(total.getLeagueRating()))
                ),
                new OverallStatDto(
                        n(total.getOverallAppearances()),
                        n(total.getOverallGoals()),
                        n(total.getOverallAssists()),
                        n(total.getOverallLeagueTitles()),
                        n(total.getOverallContinentalTitles()),
                        StatsCalculator.round2(d(total.getOverallRating()))
                )
        );
        return new PlayerDetailDto(player.getId(), player.getName(), seasonDtos, totals);
    }

    private SeasonRecordDto toSeasonDto(SeasonRecord record) {
        return new SeasonRecordDto(
                record.getId(),
                record.getSeasonName(),
                n(record.getAge()),
                new StatLineDto(
                        n(record.getContinentalAppearances()),
                        n(record.getContinentalGoals()),
                        n(record.getContinentalAssists()),
                        n(record.getContinentalTitles()),
                        StatsCalculator.round2(d(record.getContinentalRating()))
                ),
                new StatLineDto(
                        n(record.getLeagueAppearances()),
                        n(record.getLeagueGoals()),
                        n(record.getLeagueAssists()),
                        n(record.getLeagueTitles()),
                        StatsCalculator.round2(d(record.getLeagueRating()))
                )
        );
    }

    private Player requirePlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NoSuchElementException("球员不存在"));
    }

    private int nextPlayerOrder() {
        return playerRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(Player::getSortOrder)
                .mapToInt(this::n)
                .max()
                .orElse(0) + 1;
    }

    private String requireName(String name) {
        return requireText(name, "球员名称不能为空", 80);
    }

    private String requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("内容过长");
        }
        return trimmed;
    }

    private Integer sanitizeCount(Integer value, boolean skipNulls, Integer fallback) {
        if (value == null) {
            return skipNulls ? n(fallback) : 0;
        }
        if (value < 0) {
            throw new IllegalArgumentException("数值不能为负数");
        }
        return value;
    }

    private Double sanitizeRating(Double value, boolean skipNulls, Double fallback) {
        if (value == null) {
            return skipNulls ? d(fallback) : 0.0;
        }
        if (value < 0 || value > 10) {
            throw new IllegalArgumentException("均分需在 0 到 10 之间");
        }
        return StatsCalculator.round2(value);
    }

    private int sanitizeAge(Integer age) {
        int value = n(age);
        if (value < 0 || value > 60) {
            throw new IllegalArgumentException("年龄不合法");
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private double d(Double value) {
        return value == null ? 0 : value;
    }
}
