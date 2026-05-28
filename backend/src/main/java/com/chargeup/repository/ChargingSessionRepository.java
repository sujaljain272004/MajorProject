package com.chargeup.repository;

import com.chargeup.entity.ChargingSession;
import com.chargeup.entity.ChargingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    Optional<ChargingSession> findByBookingId(Long bookingId);

    @Query("""
        select cs from ChargingSession cs
        join fetch cs.booking b
        join fetch b.slot s
        join fetch s.station st
        join fetch b.user u
        where b.id = :bookingId
        """)
    Optional<ChargingSession> findDetailedByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        select cs from ChargingSession cs
        join fetch cs.booking b
        join fetch b.slot s
        join fetch s.station st
        join fetch b.user u
        where st.owner.id = :ownerId
          and cs.chargingStatus in :statuses
        order by cs.updatedAt desc
        """)
    List<ChargingSession> findOwnerLiveSessions(
        @Param("ownerId") Long ownerId,
        @Param("statuses") List<ChargingStatus> statuses
    );
}
