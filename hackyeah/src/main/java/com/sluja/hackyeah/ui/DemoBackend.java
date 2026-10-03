package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.view.DecisionResult;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.InboxRequestView;
import com.sluja.hackyeah.ui.view.NewReferral;
import com.sluja.hackyeah.ui.view.ReferralView;

import java.util.List;
import java.util.Optional;

/**
 * Port between the Thymeleaf views and the backend. The demo runs on an in-memory mock;
 * once the real REST/services are ready, a delegating implementation replaces it.
 */
public interface DemoBackend {

    List<HospitalView> hospitals();

    Optional<HospitalView> hospital(Long hospitalId);

    /** The small hospital the referring doctor works in. */
    HospitalView originHospital();

    /** Creates the referral, computes the ranking and sends wave 1. Returns the referral id. */
    Long createReferral(NewReferral referral);

    Optional<ReferralView> referral(Long referralId);

    /** All referrals, newest first. */
    List<ReferralView> referrals();

    /** Requests sent to the hospital, newest first (pending and already answered). */
    List<InboxRequestView> inbox(Long hospitalId);

    /** First accept wins: closes the referral and cancels the other pending requests. */
    DecisionResult accept(Long hospitalId, Long requestId);

    DecisionResult decline(Long hospitalId, Long requestId, ReferralRequest.DeclineReason reason);

    /** Turns on a manual availability flag; it expires on its own after the configured time. */
    void activateFlag(Long hospitalId, HospitalFlag.FlagType type);

    void clearFlag(Long hospitalId, HospitalFlag.FlagType type);

    /** Restores the starting state of the demo scenario. */
    void reset();
}
