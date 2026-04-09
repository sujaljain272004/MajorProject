package com.chargeup.repository;

import com.chargeup.entity.Booking;
import com.chargeup.entity.BookingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        select b from Booking b
        join fetch b.slot s
        join fetch s.station st
        join fetch b.user u
        where b.id = :bookingId
        """)
    Optional<Booking> findDetailedById(@Param("bookingId") Long bookingId);

    @Query("""
        select b from Booking b
        join fetch b.slot s
        join fetch s.station st
        join fetch b.user u
        where u.id = :userId
        order by b.createdAt desc
        """)
    List<Booking> findDetailedByUserId(@Param("userId") Long userId);

    @Query("""
        select b from Booking b
        join fetch b.slot s
        join fetch s.station st
        join fetch b.user u
        where st.owner.id = :ownerId
        order by b.createdAt desc
        """)
    List<Booking> findDetailedByOwnerId(@Param("ownerId") Long ownerId);

    long countBySlotStationOwnerId(Long ownerId);

    long countBySlotStationOwnerIdAndStatus(Long ownerId, BookingStatus status);

    boolean existsBySlotId(Long slotId);
}
