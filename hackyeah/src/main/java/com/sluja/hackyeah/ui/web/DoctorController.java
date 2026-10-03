package com.sluja.hackyeah.ui.web;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.ui.DemoBackend;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.ReferralView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@Controller
public class DoctorController {

    private final DemoBackend backend;

    public DoctorController(DemoBackend backend) {
        this.backend = backend;
    }

    @GetMapping("/doctor")
    public String form(Model model) {
        model.addAttribute("referralForm", new ReferralForm());
        return formPage(model);
    }

    @PostMapping("/doctor/referrals")
    public String create(@Valid @ModelAttribute ReferralForm referralForm, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return formPage(model);
        }
        Long id = backend.createReferral(referralForm.toNewReferral());
        return "redirect:/doctor/referrals/" + id;
    }

    @GetMapping("/doctor/referrals/{referralId}")
    public String referral(@PathVariable Long referralId, Model model) {
        model.addAttribute("referral", findReferral(referralId));
        return "doctor/referral";
    }

    /** Re-rendered status block, polled by the referral page (later: fetched after each SSE event). */
    @GetMapping("/fragments/referral/{referralId}")
    public String referralFragment(@PathVariable Long referralId, Model model) {
        model.addAttribute("referral", findReferral(referralId));
        return "doctor/referral :: status";
    }

    private String formPage(Model model) {
        model.addAttribute("origin", backend.originHospital());
        model.addAttribute("specialties", specialties(backend.hospitals()));
        model.addAttribute("procedures", Procedure.values());
        model.addAttribute("urgencies", Referral.Urgency.values());
        model.addAttribute("patientStates", Referral.PatientState.values());
        model.addAttribute("recentReferrals", backend.referrals());
        return "doctor/form";
    }

    private ReferralView findReferral(Long referralId) {
        return backend.referral(referralId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma zgłoszenia " + referralId));
    }

    private static Collection<String> specialties(List<HospitalView> hospitals) {
        TreeSet<String> specialties = new TreeSet<>();
        hospitals.forEach(h -> specialties.addAll(h.specialties()));
        return specialties;
    }
}
