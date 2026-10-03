package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.matching.HospitalAcceptanceStats;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import com.sluja.hackyeah.referral.service.HospitalAcceptanceStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import(HospitalAcceptanceStatsService.class)
class HospitalAcceptanceStatsServiceTest {

    @Autowired
    private ReferralRequestRepository repository;

    @Autowired
    private HospitalAcceptanceStatsService service;

    @Test
    void computesAcceptanceStatsCorrectly() {
        ReferralRequest accepted = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();
        ReferralRequest declined = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.DECLINED)
                .declineReason(ReferralRequest.DeclineReason.NO_BEDS)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();
        ReferralRequest expired = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.EXPIRED)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();

        repository.saveAll(List.of(accepted, declined, expired));

        Map<Long, HospitalAcceptanceStats> stats = service.getStats(List.of(1L));

        assertEquals(1, stats.size());
        HospitalAcceptanceStats hospitalStats = stats.get(1L);
        assertEquals(1, hospitalStats.acceptedCount());
        assertEquals(3, hospitalStats.respondedCount());
    }

    @Test
    void excludesPendingFromRespondedCount() {
        ReferralRequest accepted = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();
        ReferralRequest pending = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.PENDING)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();

        repository.saveAll(List.of(accepted, pending));

        Map<Long, HospitalAcceptanceStats> stats = service.getStats(List.of(1L));

        HospitalAcceptanceStats hospitalStats = stats.get(1L);
        assertEquals(1, hospitalStats.acceptedCount());
        assertEquals(1, hospitalStats.respondedCount());
    }

    @Test
    void excludesCancelledFromRespondedCount() {
        ReferralRequest accepted = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();
        ReferralRequest cancelled = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.CANCELLED)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();

        repository.saveAll(List.of(accepted, cancelled));

        Map<Long, HospitalAcceptanceStats> stats = service.getStats(List.of(1L));

        HospitalAcceptanceStats hospitalStats = stats.get(1L);
        assertEquals(1, hospitalStats.acceptedCount());
        assertEquals(1, hospitalStats.respondedCount());
    }

    @Test
    void handlesMultipleHospitals() {
        ReferralRequest h1Accepted = ReferralRequest.builder()
                .hospitalId(1L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.ACCEPTED)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();
        ReferralRequest h2Declined = ReferralRequest.builder()
                .hospitalId(2L)
                .wave(1)
                .status(ReferralRequest.RequestStatus.DECLINED)
                .declineReason(ReferralRequest.DeclineReason.NO_BEDS)
                .sentAt(LocalDateTime.now())
                .deadline(LocalDateTime.now().plusHours(1))
                .build();

        repository.saveAll(List.of(h1Accepted, h2Declined));

        Map<Long, HospitalAcceptanceStats> stats = service.getStats(List.of(1L, 2L));

        assertEquals(2, stats.size());
        assertEquals(1, stats.get(1L).acceptedCount());
        assertEquals(1, stats.get(1L).respondedCount());
        assertEquals(0, stats.get(2L).acceptedCount());
        assertEquals(1, stats.get(2L).respondedCount());
    }

    @Test
    void returnsZeroCountsForHospitalsWithNoRequests() {
        Map<Long, HospitalAcceptanceStats> stats = service.getStats(List.of(1L, 2L));

        assertEquals(2, stats.size());
        assertEquals(0, stats.get(1L).acceptedCount());
        assertEquals(0, stats.get(1L).respondedCount());
        assertEquals(0, stats.get(2L).acceptedCount());
        assertEquals(0, stats.get(2L).respondedCount());
    }
}
