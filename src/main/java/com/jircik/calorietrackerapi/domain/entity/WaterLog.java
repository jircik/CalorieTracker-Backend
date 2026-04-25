package com.jircik.calorietrackerapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="water_logs")
@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class WaterLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amountMl;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime loggedAt;

}
