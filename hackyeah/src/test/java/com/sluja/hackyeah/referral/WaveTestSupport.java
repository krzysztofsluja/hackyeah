package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.hospital.entity.TravelTime;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.hospital.repository.TravelTimeRepository;
import com.sluja.hackyeah.referral.dto.CreateReferralRequest;
import com.sluja.hackyeah.referral.entity.Referral;

import java.util.Set;

/**
 * Shared fixture builders for the wave tests. Hospitals are all NEUROLOGY + CT so hard constraints
 * stay out of the way; the travel minutes passed in are what drives the ranking.
 */
final class WaveTestSupport {

    private WaveTestSupport() {
    }

    static Hospital saveHospital(HospitalRepository repository, String name, int freeBeds) {
        return repository.save(Hospital.builder()
                .name(name)
                .latitude(50.0)
                .longitude(20.0)
                .totalBeds(100)
                .occupiedBeds(100 - freeBeds)
                .specialties(Set.of("NEUROLOGY"))
                .procedures(Set.of(Procedure.CT))
                .isolationCapable(false)
                .dutyPhone(WaveFlowTest.DUTY_PHONE)
                .build());
    }

    static void saveRoute(TravelTimeRepository repository, Hospital from, Hospital to, int minutes) {
        repository.save(TravelTime.builder().fromHospital(from).toHospital(to).minutes(minutes).build());
        if (!from.getId().equals(to.getId())) {
            repository.save(TravelTime.builder().fromHospital(to).toHospital(from).minutes(minutes).build());
        }
    }

    static CreateReferralRequest referral(Long originHospitalId, Referral.Urgency urgency) {
        return new CreateReferralRequest(
                "NEUROLOGY",
                Set.of(Procedure.CT),
                urgency,
                Referral.PatientState.UNSTABLE,
                false,
                "stroke symptoms since 2 h",
                originHospitalId);
    }
}
