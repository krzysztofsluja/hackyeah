package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.referral.WaveProperties;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralCandidate;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralCandidateRepository;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends requests in waves down the frozen ranking: the top K first, the next K once that wave
 * closes. Waves keep the noise down for hospitals while still being far faster than phoning round.
 */
@Service
public class WaveService {

    private final ReferralRepository referralRepository;
    private final ReferralRequestRepository requestRepository;
    private final ReferralCandidateRepository candidateRepository;
    private final WaveProperties waveProperties;

    public WaveService(ReferralRepository referralRepository,
                       ReferralRequestRepository requestRepository,
                       ReferralCandidateRepository candidateRepository,
                       WaveProperties waveProperties) {
        this.referralRepository = referralRepository;
        this.requestRepository = requestRepository;
        this.candidateRepository = candidateRepository;
        this.waveProperties = waveProperties;
    }

    /**
     * Dispatches the next slice of the ranking.
     *
     * @return the requests just created, empty when the ranking is exhausted - the caller then
     *         escalates rather than leaving the referral waiting on nobody
     */
    @Transactional
    public List<ReferralRequest> dispatchNextWave(Referral referral, LocalDateTime now) {
        int wave = requestRepository.findHighestWave(referral.getId()) + 1;
        List<ReferralCandidate> slice = sliceForWave(referral.getId(), wave);
        if (slice.isEmpty()) {
            return List.of();
        }

        LocalDateTime deadline = now.plus(waveProperties.effectiveTimeout(referral.getUrgency()));
        List<ReferralRequest> requests = slice.stream()
                .map(candidate -> ReferralRequest.builder()
                        .referral(referral)
                        .hospitalId(candidate.getHospitalId())
                        .wave(wave)
                        .status(ReferralRequest.RequestStatus.PENDING)
                        .sentAt(now)
                        .deadline(deadline)
                        .build())
                .toList();

        return requestRepository.saveAll(requests);
    }

    /**
     * Moves one stalled referral forward: either the next wave goes out, or the ranking is spent
     * and the referral is escalated to the coordinator.
     *
     * @return true when a new wave was sent
     */
    @Transactional
    public boolean advanceOrEscalate(Referral referral, LocalDateTime now) {
        if (!dispatchNextWave(referral, now).isEmpty()) {
            return true;
        }
        referralRepository.tryEscalate(referral.getId());
        return false;
    }

    private List<ReferralCandidate> sliceForWave(Long referralId, int wave) {
        List<ReferralCandidate> eligible =
                candidateRepository.findByReferralIdAndEligibleTrueOrderByRankAsc(referralId);

        int from = (wave - 1) * waveProperties.size();
        if (from >= eligible.size()) {
            return List.of();
        }
        int to = Math.min(from + waveProperties.size(), eligible.size());
        return eligible.subList(from, to);
    }
}
