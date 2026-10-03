package com.sluja.hackyeah.referral.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "referral_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "referral_id")
    private Referral referral;

    @Column(nullable = false, name = "hospital_id")
    private Long hospitalId;

    @Column(nullable = false)
    private Integer wave;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Column(name = "decline_reason")
    @Enumerated(EnumType.STRING)
    private DeclineReason declineReason;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime deadline;

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        CANCELLED
    }

    public enum DeclineReason {
        NO_BEDS,
        NO_SPECIALIST,
        EQUIPMENT_UNAVAILABLE,
        OTHER
    }

    public boolean isExpired(final LocalDateTime now) {
        return now.isAfter(deadline) && status == RequestStatus.PENDING;
    }
}
