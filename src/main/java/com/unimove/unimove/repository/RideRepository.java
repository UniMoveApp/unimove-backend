package com.unimove.unimove.repository;

import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    @Query("""
                SELECT r FROM Ride r
                WHERE r.driver = :driver
                AND r.departureTime > :now
                AND r.status = 'OPEN'
                ORDER BY r.departureTime ASC
            """)
    List<Ride> findUpcomingByDriver(@Param("driver") User driver, @Param("now") LocalDateTime now);

    @Query("""
                SELECT r FROM Ride r
                WHERE r.driver = :driver
                AND (:status IS NULL OR r.status = :status)
                ORDER BY r.departureTime DESC
            """)
    List<Ride> findByDriverAndStatus(@Param("driver") User driver, @Param("status") String status);

    @Query("""
                SELECT r FROM Ride r
                WHERE r.driver = :driver
                AND r.status = 'COMPLETED'
                ORDER BY r.departureTime DESC
            """)
    List<Ride> findCompletedByDriver(@Param("driver") User driver);

    @Query("""
                SELECT r FROM Ride r
                JOIN Booking b ON b.ride = r
                WHERE b.passenger = :passenger
                AND r.status = 'COMPLETED'
                ORDER BY r.departureTime DESC
            """)
    List<Ride> findCompletedByPassenger(@Param("passenger") User passenger);

    @Modifying
    @Query("UPDATE Ride r SET r.availableSeats = r.availableSeats - 1 WHERE r.id = :rideId AND r.availableSeats > 0")
    int decrementAvailableSeats(@Param("rideId") UUID rideId);

    @Modifying
    @Query("UPDATE Ride r SET r.availableSeats = r.availableSeats + 1 WHERE r.id = :rideId")
    int incrementAvailableSeats(@Param("rideId") UUID rideId);
}