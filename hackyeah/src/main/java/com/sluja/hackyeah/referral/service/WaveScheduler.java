package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;

import jakarta.transaction.Transactional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * One sweep instead of a timer per request: mark overdue requests EXPIRED, then push every OPEN
 * referral whose wave has closed into the next wave or escalation. Restart-safe by construction -
 * all the state it needs is the deadlines in the database.
 */
@Service
public class WaveScheduler {

    private final ReferralRepository referralRepository;
    private final ReferralRequestRepository requestRepository;
    private final WaveService waveService;

    public WaveScheduler(ReferralRepository referralRepository,
                         ReferralRequestRepository requestRepository,
                         WaveService waveService) {
        this.referralRepository = referralRepository;
        this.requestRepository = requestRepository;
        this.waveService = waveService;
    }

    @Scheduled(fixedDelayString = "${app.waves.sweep-interval-ms:2000}")
    @Transactional 
    public void sweep() {
        sweep(LocalDateTime.now());
    }

    /** Package-visible overload so tests can drive the clock instead of sleeping. */
    public void sweep(LocalDateTime now) {
        requestRepository.expireOverdue(now);

        for (Referral referral : referralRepository.findByStatus(Referral.ReferralStatus.OPEN)) {
            boolean waveStillOpen = requestRepository.existsByReferralIdAndStatus(
                    referral.getId(), ReferralRequest.RequestStatus.PENDING);
            if (!waveStillOpen) {
                waveService.advanceOrEscalate(referral, now);
            }
        }
    }
}
