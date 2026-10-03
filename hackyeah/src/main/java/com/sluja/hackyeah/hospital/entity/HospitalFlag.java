package com.sluja.hackyeah.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
        TK_DOWN(Procedure.CT),
        NEURO_AVAILABLE(),
        CATH_LAB_BUSY(Procedure.THROMBECTOMY, Procedure.PCI),
        ICU_FULL(Procedure.ICU, Procedure.VENTILATION),
        ISOLATION_WARD_UNAVAILABLE;

        @Getter
        private final List<Procedure> blockedProcedures;

        private FlagType(Procedure... blockedProcedures) {
            this.blockedProcedures = List.of(blockedProcedures);
        }
    }

    public boolean isValid(LocalDateTime now) {
        return now.isBefore(validUntil);
    }
}
