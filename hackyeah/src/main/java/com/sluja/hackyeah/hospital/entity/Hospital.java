package com.sluja.hackyeah.hospital.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
    private Set<String> procedures;

    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HospitalFlag> flags;

    @Column(nullable = false)
    private boolean isolationCapable;

    public Integer getAvailableBeds() {
        return totalBeds - occupiedBeds;
    }
}
