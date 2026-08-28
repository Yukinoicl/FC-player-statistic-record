package com.fcdata.fcdataserver.repository;

import com.fcdata.fcdataserver.entity.SeasonRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRecordRepository extends JpaRepository<SeasonRecord, Long> {

    List<SeasonRecord> findByPlayerIdOrderBySortOrderAscIdAsc(Long playerId);

    Optional<SeasonRecord> findByIdAndPlayerId(Long id, Long playerId);

    void deleteByPlayerId(Long playerId);
}
