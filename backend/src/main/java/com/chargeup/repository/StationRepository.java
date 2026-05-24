package com.chargeup.repository;

import com.chargeup.entity.Station;
import com.chargeup.entity.StationOperatingStatus;
import com.chargeup.entity.StationVerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByOwnerId(Long ownerId);

    List<Station> findByVerificationStatusAndOperatingStatus(
        StationVerificationStatus verificationStatus,
        StationOperatingStatus operatingStatus
    );

    @Query("""
        select st from Station st
        where st.verificationStatus = com.chargeup.entity.StationVerificationStatus.VERIFIED
          and st.operatingStatus = com.chargeup.entity.StationOperatingStatus.ACTIVE
          and (:city is null or lower(st.city) like lower(concat('%', :city, '%')))
          and (:pincode is null or st.pincode = :pincode)
        """)
    List<Station> findPublicStationsForSearch(@Param("city") String city, @Param("pincode") String pincode);

    @Query("""
        select count(st) > 0 from Station st
        where lower(st.name) = lower(:name)
          and st.latitude between :minLatitude and :maxLatitude
          and st.longitude between :minLongitude and :maxLongitude
          and (:ignoredId is null or st.id <> :ignoredId)
        """)
    boolean existsDuplicateLocation(
        @Param("name") String name,
        @Param("minLatitude") Double minLatitude,
        @Param("maxLatitude") Double maxLatitude,
        @Param("minLongitude") Double minLongitude,
        @Param("maxLongitude") Double maxLongitude,
        @Param("ignoredId") Long ignoredId
    );

    @Query("""
        select st from Station st
        join fetch st.owner
        where lower(st.name) = lower(:name)
          and st.latitude between :minLatitude and :maxLatitude
          and st.longitude between :minLongitude and :maxLongitude
          and (:ignoredId is null or st.id <> :ignoredId)
        """)
    Optional<Station> findDuplicateLocation(
        @Param("name") String name,
        @Param("minLatitude") Double minLatitude,
        @Param("maxLatitude") Double maxLatitude,
        @Param("minLongitude") Double minLongitude,
        @Param("maxLongitude") Double maxLongitude,
        @Param("ignoredId") Long ignoredId
    );
}
