package com.unimove.unimove.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "route_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

     @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "city_from", nullable = false)
    private String cityFrom;

    @Column(name = "city_to", nullable = false)
    private String cityTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
