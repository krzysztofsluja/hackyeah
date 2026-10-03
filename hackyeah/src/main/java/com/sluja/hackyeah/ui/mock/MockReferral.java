package com.sluja.hackyeah.ui.mock;

import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.ui.view.ExclusionReason;
import com.sluja.hackyeah.ui.view.NewReferral;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mutable in-memory referral state of the mock, with the ranking snapshot taken at creation. */
class MockReferral {

    /** Ranking row frozen at creation time, in ranking order. */
    record RankedHospital(Long hospitalId, Integer travelMinutes, int occupancyPercent, int acceptancePercent, int scorePercent) {}

    record ExcludedHospital(Long hospitalId, List<ExclusionReason> reasons) {}

    final Long id;
    final NewReferral data;
    final Long originHospitalId;
    final LocalDateTime createdAt;
    final List<RankedHospital> ranking;
    final List<ExcludedHospital> excluded;
    final List<MockRequest> requests = new ArrayList<>();
    Referral.ReferralStatus status = Referral.ReferralStatus.OPEN;
    Long acceptedHospitalId;
    int currentWave;

    MockReferral(Long id, NewReferral data, Long originHospitalId, LocalDateTime createdAt,
                 List<RankedHospital> ranking, List<ExcludedHospital> excluded) {
        this.id = id;
        this.data = data;
        this.originHospitalId = originHospitalId;
        this.createdAt = createdAt;
        this.ranking = List.copyOf(ranking);
        this.excluded = List.copyOf(excluded);
    }

    /** Ranked hospitals that have not been asked yet. */
    List<RankedHospital> notYetContacted() {
        return ranking.stream()
                .filter(r -> requests.stream().noneMatch(req -> req.hospitalId.equals(r.hospitalId())))
                .toList();
    }

    List<MockRequest> currentWaveRequests() {
        return requests.stream().filter(r -> r.wave == currentWave).toList();
    }
}
