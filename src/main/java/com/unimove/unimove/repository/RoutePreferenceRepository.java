package com.unimove.unimove.repository;

import com.unimove.unimove.model.RoutePreference;
import com.unimove.unimove.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoutePreferenceRepository extends JpaRepository<RoutePreference, UUID> {
    List<RoutePreference> findByUser(User user);
    int countByUser(User user);
    List<RoutePreference> findByCityFromIgnoreCaseAndCityToIgnoreCase(String cityFrom, String cityTo);
}
