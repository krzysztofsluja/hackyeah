package com.sluja.hackyeah.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel_time", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"from_hospital_id", "to_hospital_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_hospital_id")
    private Hospital fromHospital;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_hospital_id")
    private Hospital toHospital;

    @Column(nullable = false)
    private Integer minutes;

    public Integer getAdjustedMinutes(final Double rushHourMultiplier) {
        return (int) (minutes * rushHourMultiplier);
    }
}
