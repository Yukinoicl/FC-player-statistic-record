package com.fcdata.fcdataserver.repository;

import com.fcdata.fcdataserver.entity.Player;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findAllByOrderBySortOrderAscIdAsc();
}
