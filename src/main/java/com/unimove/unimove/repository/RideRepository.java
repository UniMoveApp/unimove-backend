package com.unimove.unimove.repository;

import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, java.util.UUID> {

    @Query("""
                SELECT r FROM Ride r
                WHERE (:driverUsername IS NULL OR r.driver.username = :driverUsername)
                AND (:departureCity IS NULL OR r.departureCity = :departureCity)
                AND (:arrivalCity IS NULL OR r.arrivalCity = :arrivalCity)
                AND (CAST(:date AS date) IS NULL OR CAST(r.departureTime AS date) = CAST(:date AS date))
                AND r.availableSeats > 0
                AND r.status = 'OPEN'
            """)
    List<Ride> search(
            @Param("driverUsername") String driverUsername,
            @Param("departureCity") String departureCity,
            @Param("arrivalCity") String arrivalCity,
            @Param("date") java.time.LocalDate date
    );

    String driver(User driver);
}