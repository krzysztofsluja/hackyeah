package com.sluja.hackyeah.referral.entity;

import com.sluja.hackyeah.hospital.entity.Procedure;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "referrals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String targetSpecialty;

    @ElementCollection
    @CollectionTable(name = "referral_procedures", joinColumns = @JoinColumn(name = "referral_id"))
    @Column(name = "procedure")
    @Enumerated(EnumType.STRING)
    private Set<Procedure> requiredProcedures;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Urgency urgency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PatientState patientState;

    @Column(nullable = false)
    private Boolean requiresIsolation;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReferralStatus status;

    @Column(name = "accepted_hospital_id")
    private Long acceptedHospitalId;

    @Column(nullable = false, name = "origin_hospital_id")
    private Long originHospitalId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Excluded: ReferralRequest points back here, so including it would recurse forever.
    @OneToMany(mappedBy = "referral", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ReferralRequest> requests;

    public enum Urgency {
        TIME_CRITICAL,
        URGENT_STABLE,
        PLANNED
    }

    public enum PatientState {
        STABLE,
        UNSTABLE,
        VENTILATED
    }

    public enum ReferralStatus {
        OPEN,
        ACCEPTED,
        ESCALATED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
