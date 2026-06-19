package com.unimove.unimove.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rides")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "departure_city", nullable = false)
    private String departureCity;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_city", nullable = false)
    private String arrivalCity;

    @Column(name = "arrival_time_est", nullable = false)
    private LocalDateTime arrivalTimeEst;

    @Column(name = "hotspot_1")
    private String hotspot1;

    @Column(name = "hotspot_2")
    private String hotspot2;

    @Column(name = "hotspot_3")
    private String hotspot3;

    @Column(name = "vehicle_model")
    private String vehicleModel;

    @Column(name = "vehicle_plate")
    private String vehiclePlate;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "travel_preferences")
    private String travelPreferences;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "OPEN";
        }
    }
}
