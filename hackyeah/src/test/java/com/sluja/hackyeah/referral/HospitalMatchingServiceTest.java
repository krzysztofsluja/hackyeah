package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.hospital.entity.TravelTime;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.hospital.repository.TravelTimeRepository;
import com.sluja.hackyeah.hospital.service.StaticMatrixTravelTimeProvider;
import com.sluja.hackyeah.matching.HospitalScore;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import com.sluja.hackyeah.referral.service.HospitalAcceptanceStatsService;
import com.sluja.hackyeah.referral.service.HospitalMatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest 
@Import({StaticMatrixTravelTimeProvider.class, HospitalAcceptanceStatsService.class, HospitalMatchingService.class})
class HospitalMatchingServiceTest {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private TravelTimeRepository travelTimeRepository;

    @Autowired
    private ReferralRequestRepository referralRequestRepository;

    @Autowired
    private HospitalMatchingService hospitalMatchingService;

    @Test
    void excludesHospitalsFailingHardConstraints() {
        Hospital origin = hospitalRepository.save(Hospital.builder()
                .name("Origin Hospital")
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital fullHospital = hospitalRepository.save(Hospital.builder()
                .name("Full Hospital")
                .latitude(50.1)
                .longitude(20.1)
                .totalBeds(100)
                .occupiedBeds(100)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital wrongSpecialty = hospitalRepository.save(Hospital.builder()
                .name("Wrong Specialty")
                .latitude(50.2)
                .longitude(20.2)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("CARDIOLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital eligible = hospitalRepository.save(Hospital.builder()
                .name("Eligible Hospital")
                .latitude(50.3)
                .longitude(20.3)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        travelTimeRepository.saveAll(List.of(
                TravelTime.builder().fromHospital(origin).toHospital(fullHospital).minutes(20).build(),
                TravelTime.builder().fromHospital(origin).toHospital(wrongSpecialty).minutes(25).build(),
                TravelTime.builder().fromHospital(origin).toHospital(eligible).minutes(15).build()
        ));

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(origin.getId())
                .urgency(Referral.Urgency.PLANNED)
                .build();

        List<HospitalScore> results = hospitalMatchingService.findRankedHospitals(referral);

        assertEquals(1, results.size());
        assertEquals(eligible.getId(), results.get(0).hospital().getId());
    }

    @Test
    void ranksByTravelTimeForTimeCritical() {
        Hospital origin = hospitalRepository.save(Hospital.builder()
                .name("Origin Hospital")
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital closeHospital = hospitalRepository.save(Hospital.builder()
                .name("Close Hospital")
                .latitude(50.1)
                .longitude(20.1)
                .totalBeds(100)
                .occupiedBeds(90)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital farHospital = hospitalRepository.save(Hospital.builder()
                .name("Far Hospital")
                .latitude(50.5)
                .longitude(20.5)
                .totalBeds(100)
                .occupiedBeds(10)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        travelTimeRepository.saveAll(List.of(
                TravelTime.builder().fromHospital(origin).toHospital(closeHospital).minutes(5).build(),
                TravelTime.builder().fromHospital(origin).toHospital(farHospital).minutes(45).build()
        ));

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(origin.getId())
                .urgency(Referral.Urgency.TIME_CRITICAL)
                .build();

        List<HospitalScore> results = hospitalMatchingService.findRankedHospitals(referral);

        assertEquals(2, results.size());
        assertEquals(closeHospital.getId(), results.get(0).hospital().getId(), "Shorter travel should rank first for TIME_CRITICAL");
        assertEquals(farHospital.getId(), results.get(1).hospital().getId());
    }

    @Test
    void ranksByCapacityForPlanned() {
        Hospital origin = hospitalRepository.save(Hospital.builder()
                .name("Origin Hospital")
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital fullButClose = hospitalRepository.save(Hospital.builder()
                .name("Full But Close")
                .latitude(50.1)
                .longitude(20.1)
                .totalBeds(100)
                .occupiedBeds(100)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital emptyButFar = hospitalRepository.save(Hospital.builder()
                .name("Empty But Far")
                .latitude(50.5)
                .longitude(20.5)
                .totalBeds(100)
                .occupiedBeds(0)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        travelTimeRepository.saveAll(List.of(
                TravelTime.builder().fromHospital(origin).toHospital(fullButClose).minutes(5).build(),
                TravelTime.builder().fromHospital(origin).toHospital(emptyButFar).minutes(45).build()
        ));

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(origin.getId())
                .urgency(Referral.Urgency.PLANNED)
                .build();

        List<HospitalScore> results = hospitalMatchingService.findRankedHospitals(referral);

        assertEquals(2, results.size());
        assertEquals(emptyButFar.getId(), results.get(0).hospital().getId(), "Better capacity should rank first for PLANNED despite longer travel");
        assertEquals(fullButClose.getId(), results.get(1).hospital().getId());
    }

    @Test
    void includesAcceptanceProbabilityInScore() {
        Hospital origin = hospitalRepository.save(Hospital.builder()
                .name("Origin Hospital")
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital highAcceptance = hospitalRepository.save(Hospital.builder()
                .name("High Acceptance")
                .latitude(50.1)
                .longitude(20.1)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital lowAcceptance = hospitalRepository.save(Hospital.builder()
                .name("Low Acceptance")
                .latitude(50.2)
                .longitude(20.2)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        travelTimeRepository.saveAll(List.of(
                TravelTime.builder().fromHospital(origin).toHospital(highAcceptance).minutes(20).build(),
                TravelTime.builder().fromHospital(origin).toHospital(lowAcceptance).minutes(20).build()
        ));

        referralRequestRepository.saveAll(List.of(
                ReferralRequest.builder()
                        .hospitalId(highAcceptance.getId())
                        .wave(1)
                        .status(ReferralRequest.RequestStatus.ACCEPTED)
                        .sentAt(LocalDateTime.now())
                        .deadline(LocalDateTime.now().plusHours(1))
                        .build(),
                ReferralRequest.builder()
                        .hospitalId(highAcceptance.getId())
                        .wave(1)
                        .status(ReferralRequest.RequestStatus.ACCEPTED)
                        .sentAt(LocalDateTime.now())
                        .deadline(LocalDateTime.now().plusHours(1))
                        .build(),
                ReferralRequest.builder()
                        .hospitalId(lowAcceptance.getId())
                        .wave(1)
                        .status(ReferralRequest.RequestStatus.DECLINED)
                        .declineReason(ReferralRequest.DeclineReason.NO_BEDS)
                        .sentAt(LocalDateTime.now())
                        .deadline(LocalDateTime.now().plusHours(1))
                        .build()
        ));

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(origin.getId())
                .urgency(Referral.Urgency.URGENT_STABLE)
                .build();

        List<HospitalScore> results = hospitalMatchingService.findRankedHospitals(referral);

        assertEquals(2, results.size());
        assertTrue(results.get(0).totalScore() > results.get(1).totalScore(), "High acceptance should score higher");
        assertEquals(highAcceptance.getId(), results.get(0).hospital().getId());
    }

    @Test
    void returnsCompleteCriterionScores() {
        Hospital origin = hospitalRepository.save(Hospital.builder()
                .name("Origin Hospital")
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        Hospital hospital = hospitalRepository.save(Hospital.builder()
                .name("Test Hospital")
                .latitude(50.1)
                .longitude(20.1)
                .totalBeds(100)
                .occupiedBeds(50)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .build());

        travelTimeRepository.save(TravelTime.builder()
                .fromHospital(origin)
                .toHospital(hospital)
                .minutes(20)
                .build());

        Referral referral = Referral.builder()
                .targetSpecialty("NEUROLOGY")
                .requiredProcedures(Set.of(Procedure.CT))
                .requiresIsolation(false)
                .originHospitalId(origin.getId())
                .urgency(Referral.Urgency.PLANNED)
                .build();

        List<HospitalScore> results = hospitalMatchingService.findRankedHospitals(referral);

        assertEquals(1, results.size());
        HospitalScore score = results.get(0);

        assertTrue(score.criterionScores().containsKey("BED_CAPACITY"));
        assertTrue(score.criterionScores().containsKey("ACCEPTANCE_PROBABILITY"));
        assertTrue(score.criterionScores().containsKey("TRAVEL_TIME"));
        assertTrue(score.totalScore() > 0);
    }
}
