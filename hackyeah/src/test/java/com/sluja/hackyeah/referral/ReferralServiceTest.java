package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.hospital.entity.TravelTime;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.hospital.repository.TravelTimeRepository;
import com.sluja.hackyeah.hospital.service.StaticMatrixTravelTimeProvider;
import com.sluja.hackyeah.referral.dto.CreateReferralRequest;
import com.sluja.hackyeah.referral.dto.ExcludedHospital;
import com.sluja.hackyeah.referral.dto.HospitalCandidate;
import com.sluja.hackyeah.referral.dto.ReferralCreatedResponse;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.service.HospitalAcceptanceStatsService;
import com.sluja.hackyeah.referral.service.HospitalMatchingService;
import com.sluja.hackyeah.referral.service.ReferralService;
import com.sluja.hackyeah.referral.service.WaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
@EnableConfigurationProperties(WaveProperties.class)
@Import({StaticMatrixTravelTimeProvider.class, HospitalAcceptanceStatsService.class,
        HospitalMatchingService.class, ReferralService.class, WaveService.class})
class ReferralServiceTest {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private TravelTimeRepository travelTimeRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private ReferralService referralService;

    private Hospital saveHospital(String name, int totalBeds, int occupiedBeds,
                                  Set<String> specialties, Set<Procedure> procedures,
                                  boolean isolationCapable) {
        return hospitalRepository.save(Hospital.builder()
                .name(name)
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(totalBeds)
                .occupiedBeds(occupiedBeds)
                .specialties(specialties)
                .procedures(procedures)
                .isolationCapable(isolationCapable)
                .build());
    }

    /** Routes are directional rows, so both directions are needed - unless it is a self-route. */
    private void saveRoute(Hospital from, Hospital to, int minutes) {
        travelTimeRepository.save(TravelTime.builder().fromHospital(from).toHospital(to).minutes(minutes).build());
        if (!from.getId().equals(to.getId())) {
            travelTimeRepository.save(TravelTime.builder().fromHospital(to).toHospital(from).minutes(minutes).build());
        }
    }

    private CreateReferralRequest neurologyReferral(Long originHospitalId, boolean requiresIsolation) {
        return new CreateReferralRequest(
                "NEUROLOGY",
                Set.of(Procedure.CT),
                Referral.Urgency.TIME_CRITICAL,
                Referral.PatientState.UNSTABLE,
                requiresIsolation,
                "left-sided weakness",
                originHospitalId);
    }

    @Test
    void persistsReferralAndRanksCandidatesByTravelTime() {
        Hospital origin = saveHospital("Origin", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital near = saveHospital("Near", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital far = saveHospital("Far", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        saveRoute(origin, origin, 0);
        saveRoute(origin, near, 10);
        saveRoute(origin, far, 40);

        ReferralCreatedResponse response = referralService.create(neurologyReferral(origin.getId(), false), 5);

        assertNotNull(response.id());
        assertEquals(Referral.ReferralStatus.OPEN, response.status());
        assertNotNull(response.createdAt());
        assertEquals(1, response.wave());
        assertEquals(3, response.requests().size());

        Optional<Referral> saved = referralRepository.findById(response.id());
        assertTrue(saved.isPresent());
        assertEquals("NEUROLOGY", saved.get().getTargetSpecialty());
        assertEquals(Set.of(Procedure.CT), saved.get().getRequiredProcedures());
        assertEquals(origin.getId(), saved.get().getOriginHospitalId());

        // TIME_CRITICAL weighs travel time at 0.7, so the nearer hospital must rank higher.
        assertEquals(List.of("Origin", "Near", "Far"),
                response.candidates().stream().map(HospitalCandidate::name).toList());

        HospitalCandidate nearCandidate = response.candidates().get(1);
        assertEquals(10, nearCandidate.travelMinutes());
        assertEquals(50, nearCandidate.availableBeds());
        assertEquals(Set.of("TRAVEL_TIME", "BED_CAPACITY", "ACCEPTANCE_PROBABILITY"),
                nearCandidate.criterionScores().keySet());
        assertTrue(response.excluded().isEmpty());
    }

    @Test
    void reportsExcludedHospitalsWithViolationCodes() {
        Hospital origin = saveHospital("Origin", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital full = saveHospital("Full", 100, 100, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital wrongSpecialty = saveHospital("Wrong Specialty", 100, 50, Set.of("CARDIOLOGY"), Set.of(Procedure.CT), false);
        saveRoute(origin, origin, 0);
        saveRoute(origin, full, 10);
        saveRoute(origin, wrongSpecialty, 10);

        ReferralCreatedResponse response = referralService.create(neurologyReferral(origin.getId(), false), 5);

        assertEquals(List.of("Origin"), response.candidates().stream().map(HospitalCandidate::name).toList());
        assertEquals(2, response.excluded().size());
        assertEquals(List.of("NO_AVAILABLE_BEDS"), violationsOf(response, full.getId()));
        assertEquals(List.of("SPECIALTY_NOT_COVERED"), violationsOf(response, wrongSpecialty.getId()));
    }

    @Test
    void escalatesWhenNothingIsEligible() {
        Hospital origin = saveHospital("Origin", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital other = saveHospital("Other", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        saveRoute(origin, origin, 0);
        saveRoute(origin, other, 10);

        // No hospital is isolation capable, so the isolation rule drops all of them.
        ReferralCreatedResponse response = referralService.create(neurologyReferral(origin.getId(), true), 5);

        assertTrue(response.candidates().isEmpty());
        assertEquals(2, response.excluded().size());
        assertTrue(response.excluded().stream()
                .allMatch(excluded -> excluded.violations().contains("ISOLATION_NOT_CAPABLE")));

        // Nobody to ask, so the referral goes straight to the coordinator.
        Referral saved = referralRepository.findById(response.id()).orElseThrow();
        assertEquals(Referral.ReferralStatus.ESCALATED, saved.getStatus());
        assertNull(saved.getAcceptedHospitalId());
    }

    @Test
    void limitTruncatesCandidatesButNotExcluded() {
        Hospital origin = saveHospital("Origin", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital near = saveHospital("Near", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital far = saveHospital("Far", 100, 50, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital full = saveHospital("Full", 100, 100, Set.of("NEUROLOGY"), Set.of(Procedure.CT), false);
        Hospital wrongSpecialty = saveHospital("Wrong Specialty", 100, 50, Set.of("CARDIOLOGY"), Set.of(Procedure.CT), false);
        saveRoute(origin, origin, 0);
        saveRoute(origin, near, 10);
        saveRoute(origin, far, 40);
        saveRoute(origin, full, 10);
        saveRoute(origin, wrongSpecialty, 10);

        ReferralCreatedResponse response = referralService.create(neurologyReferral(origin.getId(), false), 1);

        assertEquals(1, response.candidates().size());
        assertEquals(2, response.excluded().size());
    }

    @Test
    void rejectsUnknownOriginHospital() {
        assertThrows(IllegalArgumentException.class,
                () -> referralService.create(neurologyReferral(999L, false), 5));
    }

    @Test
    void acceptsReferralWithoutRequiredProcedures() {
        Hospital origin = saveHospital("Origin", 100, 50, Set.of("NEUROLOGY"), Set.of(), false);
        saveRoute(origin, origin, 0);

        CreateReferralRequest request = new CreateReferralRequest(
                "NEUROLOGY", null, Referral.Urgency.PLANNED, Referral.PatientState.STABLE,
                false, null, origin.getId());

        ReferralCreatedResponse response = referralService.create(request, 5);

        assertEquals(1, response.candidates().size());
        assertEquals(Set.of(), referralRepository.findById(response.id()).orElseThrow().getRequiredProcedures());
    }

    private static List<String> violationsOf(ReferralCreatedResponse response, Long hospitalId) {
        return response.excluded().stream()
                .filter(excluded -> excluded.hospitalId().equals(hospitalId))
                .map(ExcludedHospital::violations)
                .findFirst()
                .orElseThrow();
    }
}
