package com.fcdata.fcdataserver.controller;

import com.fcdata.fcdataserver.config.LocalAppSupport;
import com.fcdata.fcdataserver.dto.ApiDtos.CreatePlayerRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.PlayerDetailDto;
import com.fcdata.fcdataserver.dto.ApiDtos.PlayerSummaryDto;
import com.fcdata.fcdataserver.dto.ApiDtos.RenamePlayerRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.ReorderPlayersRequest;
import com.fcdata.fcdataserver.dto.ApiDtos.SeasonUpsertRequest;
import com.fcdata.fcdataserver.service.PlayerService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        if (LocalAppSupport.packaged()) {
            return Map.of("status", "ok", "app", "fc-data-record", "mode", "packaged");
        }
        return Map.of("status", "ok", "app", "fc-data-record");
    }

    @GetMapping("/players")
    public List<PlayerSummaryDto> listPlayers() {
        return playerService.listPlayers();
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerDetailDto createPlayer(@RequestBody(required = false) CreatePlayerRequest request) {
        return playerService.createPlayer(request);
    }

    @PutMapping("/players/order")
    public List<PlayerSummaryDto> reorderPlayers(@RequestBody ReorderPlayersRequest request) {
        return playerService.reorderPlayers(request);
    }

    @GetMapping("/players/{id}")
    public PlayerDetailDto getPlayer(@PathVariable Long id) {
        return playerService.getPlayer(id);
    }

    @PutMapping("/players/{id}")
    public PlayerSummaryDto renamePlayer(
            @PathVariable Long id,
            @RequestBody RenamePlayerRequest request
    ) {
        return playerService.renamePlayer(id, request);
    }

    @DeleteMapping("/players/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
    }

    @PostMapping("/players/{id}/seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerDetailDto addSeason(
            @PathVariable Long id,
            @RequestBody(required = false) SeasonUpsertRequest request
    ) {
        return playerService.addSeason(id, request);
    }

    @PutMapping("/players/{id}/seasons/{seasonId}")
    public PlayerDetailDto updateSeason(
            @PathVariable Long id,
            @PathVariable Long seasonId,
            @RequestBody SeasonUpsertRequest request
    ) {
        return playerService.updateSeason(id, seasonId, request);
    }

    @DeleteMapping("/players/{id}/seasons/{seasonId}")
    public PlayerDetailDto deleteSeason(@PathVariable Long id, @PathVariable Long seasonId) {
        return playerService.deleteSeason(id, seasonId);
    }
}
