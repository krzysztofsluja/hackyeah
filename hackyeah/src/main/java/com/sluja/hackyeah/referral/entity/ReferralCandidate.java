package com.sluja.hackyeah.referral.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * The ranking frozen when the referral is filed. Waves walk down it by {@code rank}, so the order
 * a doctor sees never shifts underneath them. Hospitals that failed a hard constraint are stored
 * too, with {@code eligible = false} and their violation codes, to explain the gaps in the list.
 */
@Entity
@Table(name = "referral_candidates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"referral_id", "hospital_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "referral_id")
    private Referral referral;

    @Column(nullable = false, name = "hospital_id")
    private Long hospitalId;

    @Column(nullable = false)
    private String hospitalName;

    /** 1-based position among the eligible hospitals; null when this candidate was excluded. */
    @Column(name = "candidate_rank")
    private Integer rank;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(nullable = false)
    private boolean eligible;

    @ElementCollection
    @CollectionTable(name = "referral_candidate_scores",
            joinColumns = @JoinColumn(name = "candidate_id"))
    @MapKeyColumn(name = "criterion")
    @Column(name = "score")
    private Map<String, Double> criterionScores;

    @ElementCollection
    @CollectionTable(name = "referral_candidate_violations",
            joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "violation")
    private List<String> violations;
}
