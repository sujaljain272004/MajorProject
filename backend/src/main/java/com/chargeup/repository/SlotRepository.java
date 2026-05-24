package com.chargeup.repository;

import com.chargeup.entity.Slot;
import com.chargeup.entity.SlotState;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByStationIdOrderByStartTimeAsc(Long stationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :slotId")
    Optional<Slot> findByIdForUpdate(@Param("slotId") Long slotId);

    long countByStationOwnerId(Long ownerId);

    long countByStationOwnerIdAndAvailableTrue(Long ownerId);

    boolean existsByStationIdAndStartTimeAndEndTime(Long stationId, LocalDateTime startTime, LocalDateTime endTime);

    long countByStationIdAndState(Long stationId, SlotState state);
}
