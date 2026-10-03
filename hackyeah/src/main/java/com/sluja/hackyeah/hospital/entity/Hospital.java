package com.sluja.hackyeah.hospital.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;

@Entity 
@Table (name = "hospitals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** City district shown next to the name, so people recognise the hospital at a glance. */
    private String district;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer totalBeds;

    @Column(nullable = false)
    private Integer occupiedBeds;

    @ElementCollection 
    @CollectionTable(name = "hospital_specialties", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "specialties")
    private Set<String> specialties;

    @ElementCollection
    @CollectionTable(name = "hospital_procedures", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "procedures")
    @Enumerated(EnumType.STRING)
    private Set<Procedure> procedures;

    // Excluded: HospitalFlag points back here, so including it would recurse forever.
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<HospitalFlag> flags;

    @Column(nullable = false)
    private boolean isolationCapable;

    /**
     * Institutional on-call line, never a named doctor's number - the person on duty rotates and
     * naming them would make this personal data. Handed over only after an accept or an escalation.
     */
    @Column(name = "duty_phone")
    private String dutyPhone;

    public Integer getAvailableBeds() {
        return totalBeds - occupiedBeds;
    }
}
