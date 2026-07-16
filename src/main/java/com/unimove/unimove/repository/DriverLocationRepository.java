package com.unimove.unimove.repository;

import com.unimove.unimove.model.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, UUID> {
}
