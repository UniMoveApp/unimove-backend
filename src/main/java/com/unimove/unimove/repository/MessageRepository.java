package com.unimove.unimove.repository;

import com.unimove.unimove.model.Message;
import com.unimove.unimove.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByRideOrderByCreatedAtAsc(Ride ride);

    void deleteByRide(Ride ride);
}
