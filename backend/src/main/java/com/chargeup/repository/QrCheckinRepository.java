package com.chargeup.repository;

import com.chargeup.entity.QrCheckin;
import com.chargeup.entity.QrVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCheckinRepository extends JpaRepository<QrCheckin, Long> {

    boolean existsByBookingIdAndVerificationStatus(Long bookingId, QrVerificationStatus verificationStatus);
}
