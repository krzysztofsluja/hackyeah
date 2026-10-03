package com.sluja.hackyeah.ui.web;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.DemoBackend;
import com.sluja.hackyeah.ui.view.DecisionResult;
import com.sluja.hackyeah.ui.view.HospitalView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HospitalController {

    private final DemoBackend backend;

    public HospitalController(DemoBackend backend) {
        this.backend = backend;
    }

    @GetMapping("/hospital/{hospitalId}")
    public String inbox(@PathVariable Long hospitalId, Model model) {
        addInbox(hospitalId, model);
        model.addAttribute("flagTypes", HospitalFlag.FlagType.values());
        model.addAttribute("declineReasons", ReferralRequest.DeclineReason.values());
        return "hospital/inbox";
    }

    /** Re-rendered inbox, polled by the hospital page (later: fetched after each SSE event). */
    @GetMapping("/fragments/hospital/{hospitalId}/inbox")
    public String inboxFragment(@PathVariable Long hospitalId, Model model) {
        addInbox(hospitalId, model);
        model.addAttribute("declineReasons", ReferralRequest.DeclineReason.values());
        return "hospital/inbox :: requests";
    }

    @PostMapping("/hospital/{hospitalId}/requests/{requestId}/accept")
    public String accept(@PathVariable Long hospitalId, @PathVariable Long requestId, RedirectAttributes redirect) {
        DecisionResult result = backend.accept(hospitalId, requestId);
        flash(redirect, result, "Pacjent przyjęty. Skontaktuj się z lekarzem zlecającym — numer poniżej.");
        return "redirect:/hospital/" + hospitalId;
    }

    @PostMapping("/hospital/{hospitalId}/requests/{requestId}/decline")
    public String decline(@PathVariable Long hospitalId, @PathVariable Long requestId,
                          @RequestParam ReferralRequest.DeclineReason reason, RedirectAttributes redirect) {
        DecisionResult result = backend.decline(hospitalId, requestId, reason);
        flash(redirect, result, "Odmowa zapisana. Zgłoszenie trafi do kolejnych szpitali.");
        return "redirect:/hospital/" + hospitalId;
    }

    @PostMapping("/hospital/{hospitalId}/flags/{type}")
    public String toggleFlag(@PathVariable Long hospitalId, @PathVariable HospitalFlag.FlagType type,
                             @RequestParam boolean active) {
        findHospital(hospitalId);
        if (active) {
            backend.activateFlag(hospitalId, type);
        } else {
            backend.clearFlag(hospitalId, type);
        }
        return "redirect:/hospital/" + hospitalId;
    }

    private void addInbox(Long hospitalId, Model model) {
        model.addAttribute("hospital", findHospital(hospitalId));
        model.addAttribute("requests", backend.inbox(hospitalId));
    }

    private HospitalView findHospital(Long hospitalId) {
        return backend.hospital(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma szpitala " + hospitalId));
    }

    private static void flash(RedirectAttributes redirect, DecisionResult result, String successMessage) {
        switch (result) {
            case OK -> redirect.addFlashAttribute("flash", successMessage);
            case ALREADY_TAKEN -> {
                redirect.addFlashAttribute("flash", "Zgłoszenie już zrealizowane — przyjął je inny szpital.");
                redirect.addFlashAttribute("flashType", "warn");
            }
            case NO_LONGER_PENDING -> {
                redirect.addFlashAttribute("flash", "To zapytanie jest już zamknięte (odpowiedź udzielona lub minął czas).");
                redirect.addFlashAttribute("flashType", "warn");
            }
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego zapytania");
        }
    }
}
