package com.sluja.hackyeah.matching;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.referral.entity.Referral;
import org.junit.jupiter.api.Test;
import com.sluja.hackyeah.hospital.entity.Procedure;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardConstraintFilterTest {

    private final TravelTimeProvider mockProvider = (from, to) -> java.util.Optional.of(20);
    private final HardConstraintFilter filter = HardConstraintFilter.standard(mockProvider);
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void allHospitalsPassWhenReferralHasMinimalRequirements() {
        Hospital h1 = Hospital.builder()
                .id(1L)
                .name("Hospital 1")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();
        Hospital h2 = Hospital.builder()
                .id(2L)
                .name("Hospital 2")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(h1, h2), now);

        assertEquals(2, eligible.size());
    }

    @Test
    void noHospitalsPassWhenSpecialtyMissing() {
        Hospital h1 = Hospital.builder()
                .id(1L)
                .name("Hospital 1")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("CARDIOLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(h1), now);

        assertEquals(0, eligible.size());
    }

    @Test
    void someHospitalsPassWhenSomeFailBeds() {
        Hospital h1 = Hospital.builder()
                .id(1L)
                .name("Hospital 1 - Full")
                .totalBeds(100)
                .occupiedBeds(100)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();
        Hospital h2 = Hospital.builder()
                .id(2L)
                .name("Hospital 2 - Available")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(h1, h2), now);

        assertEquals(1, eligible.size());
        assertEquals(2L, eligible.get(0).getId());
    }

    @Test
    void evaluateReturnsAllViolations() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Test Hospital")
                .totalBeds(100)
                .occupiedBeds(100)
                .specialties(Set.of("CARDIOLOGY"))
                .procedures(Set.of(Procedure.MRI))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT, Procedure.THROMBOLYSIS))
                .requiresIsolation(true)
                .build();

        List<HospitalEligibility> results = filter.evaluate(referral, List.of(hospital), now);

        assertEquals(1, results.size());
        HospitalEligibility result = results.get(0);
        assertFalse(result.eligible());
        assertTrue(result.violations().size() >= 3);
        assertTrue(result.violations().stream().anyMatch(v -> v.equals("NO_AVAILABLE_BEDS")));
        assertTrue(result.violations().stream().anyMatch(v -> v.equals("SPECIALTY_NOT_COVERED")));
        assertTrue(result.violations().stream().anyMatch(v -> v.equals("ISOLATION_NOT_CAPABLE")));
    }

    private void assertFalse(boolean eligible) {
        assertTrue(!eligible);
    }

    @Test
    void isolationRequirementFiltering() {
        Hospital isolationCapable = Hospital.builder()
                .id(1L)
                .name("Isolation Capable")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("INFECTIOUS_DISEASES"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(true)
                .build();
        Hospital notIsolationCapable = Hospital.builder()
                .id(2L)
                .name("Not Isolation Capable")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("INFECTIOUS_DISEASES"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("INFECTIOUS_DISEASES")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(true)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(isolationCapable, notIsolationCapable), now);

        assertEquals(1, eligible.size());
        assertEquals(1L, eligible.get(0).getId());
    }

    @Test
    void blockingFlagExclusion() {
        HospitalFlag tkDownFlag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.TK_DOWN)
                .validUntil(now.plusHours(1))
                .build();
        Hospital blockedHospital = Hospital.builder()
                .id(1L)
                .name("Hospital with TK Down")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .flags(Set.of(tkDownFlag))
                .build();
        Hospital availableHospital = Hospital.builder()
                .id(2L)
                .name("Hospital without flags")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .flags(Set.of())
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(blockedHospital, availableHospital), now);

        assertEquals(1, eligible.size());
        assertEquals(2L, eligible.get(0).getId());
    }

    @Test
    void procedureCoveragePartialMatch() {
        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Partial Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT, Procedure.MRI))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT, Procedure.THROMBOLYSIS))
                .requiresIsolation(false)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(hospital), now);

        assertEquals(0, eligible.size());
    }

    @Test
    void excludeHospitalWithoutTravelRoute() {
        TravelTimeProvider noRoute = (from, to) -> java.util.Optional.empty();
        HardConstraintFilter restrictiveFilter = HardConstraintFilter.standard(noRoute);

        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Unreachable Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(2L)
                .build();

        List<Hospital> eligible = restrictiveFilter.filterEligible(referral, List.of(hospital), now);

        assertEquals(0, eligible.size());
    }

    @Test
    void includedHospitalWithValidTravelRoute() {
        TravelTimeProvider hasRoute = (from, to) -> java.util.Optional.of(25);
        HardConstraintFilter routeAwareFilter = HardConstraintFilter.standard(hasRoute);

        Hospital hospital = Hospital.builder()
                .id(1L)
                .name("Reachable Hospital")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(2L)
                .build();

        List<Hospital> eligible = routeAwareFilter.filterEligible(referral, List.of(hospital), now);

        assertEquals(1, eligible.size());
    }

    @Test
    void excludeHospitalWithBlockedProcedure() {
        HospitalFlag cathLabBusyFlag = HospitalFlag.builder()
                .id(1L)
                .type(HospitalFlag.FlagType.CATH_LAB_BUSY)
                .validUntil(now.plusHours(1))
                .build();
        Hospital blockedHospital = Hospital.builder()
                .id(1L)
                .name("Hospital with Cath Lab Busy")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("CARDIOLOGY"))
                .procedures(Set.of(Procedure.CT, Procedure.PCI, Procedure.MRI))
                .isolationCapable(false)
                .flags(Set.of(cathLabBusyFlag))
                .build();
        Hospital availableHospital = Hospital.builder()
                .id(2L)
                .name("Hospital without flags")
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("CARDIOLOGY"))
                .procedures(Set.of(Procedure.CT, Procedure.PCI, Procedure.MRI))
                .isolationCapable(false)
                .flags(Set.of())
                .build();

        Referral referral = Referral.builder()
                .targetSpecialty("CARDIOLOGY")
                .requiredProcedures(Set.of(Procedure.PCI))
                .requiresIsolation(false)
                .build();

        List<Hospital> eligible = filter.filterEligible(referral, List.of(blockedHospital, availableHospital), now);

        assertEquals(1, eligible.size());
        assertEquals(2L, eligible.get(0).getId());
    }
}
