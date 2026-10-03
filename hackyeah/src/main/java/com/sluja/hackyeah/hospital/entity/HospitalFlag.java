package com.sluja.hackyeah.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hospital_flags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FlagType type;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    public enum FlagType {
        TK_DOWN,
        NEURO_AVAILABLE,
        CATH_LAB_BUSY,
        ICU_FULL,
        ISOLATION_WARD_UNAVAILABLE
    }

    public boolean isValid(LocalDateTime now) {
        return now.isBefore(validUntil);
    }
}
