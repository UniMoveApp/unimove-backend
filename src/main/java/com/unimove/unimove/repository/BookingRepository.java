package com.unimove.unimove.repository;

import com.unimove.unimove.model.Booking;
import com.unimove.unimove.model.Ride;
import com.unimove.unimove.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    boolean existsByRideAndPassenger(Ride ride, User passenger);

    boolean existsByRideAndPassengerAndStatus(Ride ride, User passenger, String status);

    List<Booking> findByPassenger(User passenger);

    Optional<Booking> findByIdAndPassenger(UUID id, User passenger);

    List<Booking> findByRide(Ride ride);
}
