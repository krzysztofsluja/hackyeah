package com.sluja.hackyeah.referral.service;

import com.sluja.hackyeah.hospital.entity.Hospital;
import com.sluja.hackyeah.hospital.repository.HospitalRepository;
import com.sluja.hackyeah.referral.dto.AcceptanceResponse;
import com.sluja.hackyeah.referral.dto.HospitalContact;
import com.sluja.hackyeah.referral.dto.InboxEntry;
import com.sluja.hackyeah.referral.dto.RequestView;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.repository.ReferralRepository;
import com.sluja.hackyeah.referral.repository.ReferralRequestRepository;
import com.sluja.hackyeah.web.ConflictException;
import com.sluja.hackyeah.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** How a receiving hospital answers a request: accept (first one wins) or decline with a reason. */
@Service
public class RequestResponseService {

    private final ReferralRepository referralRepository;
    private final ReferralRequestRepository requestRepository;
    private final HospitalRepository hospitalRepository;

    public RequestResponseService(ReferralRepository referralRepository,
                                  ReferralRequestRepository requestRepository,
                                  HospitalRepository hospitalRepository) {
        this.referralRepository = referralRepository;
        this.requestRepository = requestRepository;
        this.hospitalRepository = hospitalRepository;
    }

    /** The open requests waiting on this hospital, newest first. */
    @Transactional(readOnly = true)
    public List<InboxEntry> inbox(Long hospitalId) {
        return requestRepository
                .findByHospitalIdAndStatusOrderBySentAtDesc(hospitalId, ReferralRequest.RequestStatus.PENDING)
                .stream()
                .map(RequestResponseService::toInboxEntry)
                .toList();
    }

    /**
     * Claims the referral for this request's hospital. The conditional update on the referral is
     * the race winner, so a simultaneous second accept loses cleanly instead of double-booking.
     * Both sides get an on-call number back - acceptance is where the phone call becomes useful.
     *
     * @throws ConflictException when the referral has already left OPEN, or this request is no
     *                           longer pending
     */
    @Transactional
    public AcceptanceResponse accept(Long requestId) {
        ReferralRequest request = require(requestId);
        requirePending(request);

        Long referralId = request.getReferral().getId();
        Long originHospitalId = request.getReferral().getOriginHospitalId();
        if (referralRepository.tryAccept(referralId, request.getHospitalId()) == 0) {
            throw new ConflictException("Referral " + referralId + " has already been settled");
        }

        requestRepository.cancelLosers(referralId, requestId);

        // Re-read: the bulk updates above cleared the persistence context.
        ReferralRequest winner = require(requestId);
        winner.setStatus(ReferralRequest.RequestStatus.ACCEPTED);
        ReferralRequest saved = requestRepository.save(winner);

        return new AcceptanceResponse(
                toView(saved),
                contactFor(saved.getHospitalId()),
                contactFor(originHospitalId));
    }

    @Transactional
    public RequestView decline(Long requestId, ReferralRequest.DeclineReason reason) {
        ReferralRequest request = require(requestId);
        requirePending(request);

        request.setStatus(ReferralRequest.RequestStatus.DECLINED);
        request.setDeclineReason(reason);
        return toView(requestRepository.save(request));
    }

    private HospitalContact contactFor(Long hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .map(hospital -> new HospitalContact(
                        hospital.getId(), hospital.getName(), hospital.getDutyPhone()))
                .orElse(null);
    }

    private RequestView toView(ReferralRequest request) {
        Optional<Hospital> hospital = hospitalRepository.findById(request.getHospitalId());
        return new RequestView(
                request.getId(),
                request.getHospitalId(),
                hospital.map(Hospital::getName).orElse(null),
                request.getWave(),
                request.getStatus(),
                request.getDeclineReason(),
                request.getSentAt(),
                request.getDeadline());
    }

    private static InboxEntry toInboxEntry(ReferralRequest request) {
        Referral referral = request.getReferral();
        return new InboxEntry(
                request.getId(),
                referral.getId(),
                request.getWave(),
                request.getDeadline(),
                referral.getTargetSpecialty(),
                referral.getRequiredProcedures(),
                referral.getUrgency(),
                referral.getPatientState(),
                Boolean.TRUE.equals(referral.getRequiresIsolation()),
                referral.getNote());
    }

    private ReferralRequest require(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("No referral request " + requestId));
    }

    private void requirePending(ReferralRequest request) {
        if (request.getStatus() != ReferralRequest.RequestStatus.PENDING) {
            throw new ConflictException("Request " + request.getId()
                    + " is already " + request.getStatus());
        }
        Referral referral = request.getReferral();
        if (referral.getStatus() != Referral.ReferralStatus.OPEN) {
            throw new ConflictException("Referral " + referral.getId()
                    + " is already " + referral.getStatus());
        }
    }
}
