package com.chargeup.repository;

import com.chargeup.entity.Station;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByOwnerId(Long ownerId);
}
