package com.chargeup.repository;

import com.chargeup.entity.Payment;
import com.chargeup.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    @Query("""
        select coalesce(sum(p.amount), 0) from Payment p
        where p.status = :status and p.booking.status = com.chargeup.entity.BookingStatus.CONFIRMED
        and p.booking.slot.station.owner.id = :ownerId
        """)
    BigDecimal sumRevenueByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") PaymentStatus status);
}
