package com.chargeup.config;

import com.chargeup.entity.Role;
import com.chargeup.entity.Slot;
import com.chargeup.entity.Station;
import com.chargeup.entity.User;
import com.chargeup.repository.SlotRepository;
import com.chargeup.repository.StationRepository;
import com.chargeup.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
        UserRepository userRepository,
        StationRepository stationRepository,
        SlotRepository slotRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User owner = new User();
            owner.setName("ChargeUp Owner");
            owner.setEmail("owner@chargeup.com");
            owner.setPassword(passwordEncoder.encode("owner123"));
            owner.setRole(Role.OWNER);
            owner = userRepository.save(owner);

            User driver = new User();
            driver.setName("ChargeUp Driver");
            driver.setEmail("driver@chargeup.com");
            driver.setPassword(passwordEncoder.encode("driver123"));
            driver.setRole(Role.DRIVER);
            userRepository.save(driver);

            Station stationOne = new Station();
            stationOne.setName("ChargeUp Downtown Hub");
            stationOne.setLocation("MG Road, Bengaluru");
            stationOne.setLatitude(12.9716);
            stationOne.setLongitude(77.5946);
            stationOne.setOwner(owner);
            stationOne = stationRepository.save(stationOne);

            Station stationTwo = new Station();
            stationTwo.setName("ChargeUp Tech Park");
            stationTwo.setLocation("Whitefield, Bengaluru");
            stationTwo.setLatitude(12.9698);
            stationTwo.setLongitude(77.7500);
            stationTwo.setOwner(owner);
            stationTwo = stationRepository.save(stationTwo);

            seedSlot(slotRepository, stationOne, 1, 9, "499.00");
            seedSlot(slotRepository, stationOne, 1, 11, "549.00");
            seedSlot(slotRepository, stationTwo, 1, 10, "449.00");
            seedSlot(slotRepository, stationTwo, 1, 12, "599.00");
        };
    }

    private void seedSlot(SlotRepository slotRepository, Station station, int dayOffset, int startHour, String price) {
        LocalDateTime start = LocalDateTime.now().plusDays(dayOffset).withHour(startHour).withMinute(0).withSecond(0).withNano(0);
        Slot slot = new Slot();
        slot.setStation(station);
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setPrice(new BigDecimal(price));
        slot.setAvailable(true);
        slotRepository.save(slot);
    }
}
