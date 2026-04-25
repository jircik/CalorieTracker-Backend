package com.jircik.calorietrackerapi.repository;

import com.jircik.calorietrackerapi.domain.entity.WaterLog;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WaterLogRepository extends CrudRepository<WaterLog, Long> {
    List<WaterLog> findByUser_IdAndLoggedAtBetweenOrderByLoggedAtAsc(
            Long userId, LocalDateTime from, LocalDateTime to);
}
