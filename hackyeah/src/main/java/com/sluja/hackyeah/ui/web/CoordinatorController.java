package com.sluja.hackyeah.ui.web;

import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.DemoBackend;
import com.sluja.hackyeah.ui.view.CoordinatorOverview;
import com.sluja.hackyeah.ui.view.HospitalView;
import com.sluja.hackyeah.ui.view.InboxRequestView;
import com.sluja.hackyeah.ui.view.MapHospital;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CoordinatorController {

    private final DemoBackend backend;
    private final MessageSource messages;

    public CoordinatorController(DemoBackend backend, MessageSource messages) {
        this.backend = backend;
        this.messages = messages;
    }

    @GetMapping("/coordinator")
    public String dashboard(Model model) {
        model.addAttribute("overview", overview());
        return "coordinator/dashboard";
    }

    /** Alerts and escalations next to the map — what the coordinator acts on. */
    @GetMapping("/fragments/coordinator/actions")
    public String actionsFragment(Model model) {
        model.addAttribute("overview", overview());
        return "coordinator/dashboard :: actions";
    }

    /** Active referrals and the decline counter. */
    @GetMapping("/fragments/coordinator/flow")
    public String flowFragment(Model model) {
        model.addAttribute("overview", overview());
        return "coordinator/dashboard :: flow";
    }

    /** Hospital table under the map, polled by the dashboard (later: fetched after each SSE event). */
    @GetMapping("/fragments/coordinator/hospitals")
    public String hospitalsFragment() {
        return "coordinator/dashboard :: hospitals";
    }

    /** Map data, fetched by the map every few seconds; markers are updated in place. */
    @GetMapping("/coordinator/api/dashboard")
    @ResponseBody
    public List<MapHospital> mapData() {
        Locale locale = LocaleContextHolder.getLocale();
        Long originId = backend.originHospital().id();
        return backend.hospitals().stream()
                .map(h -> toMapHospital(h, originId, locale))
                .toList();
    }

    private MapHospital toMapHospital(HospitalView hospital, Long originId, Locale locale) {
        List<InboxRequestView> requests = backend.inbox(hospital.id());

        int pending = (int) requests.stream().filter(InboxRequestView::isPending).count();
        Map<ReferralRequest.DeclineReason, Long> declinesByReason = requests.stream()
                .filter(r -> r.declineReason() != null)
                .collect(Collectors.groupingBy(InboxRequestView::declineReason,
                        () -> new EnumMap<>(ReferralRequest.DeclineReason.class), Collectors.counting()));

        return new MapHospital(hospital.id(), hospital.name(), hospital.district(),
                hospital.latitude(), hospital.longitude(), hospital.totalBeds(), hospital.occupiedBeds(),
                hospital.occupancyPercent(), hospital.occupancyLevel().name().toLowerCase(Locale.ROOT),
                hospital.id().equals(originId),
                hospital.flags().stream().map(f -> message("flag." + f.type(), locale)).toList(),
                pending,
                declinesByReason.entrySet().stream()
                        .map(e -> new MapHospital.LabeledCount(message("declineReason." + e.getKey(), locale), e.getValue()))
                        .toList());
    }

    private CoordinatorOverview overview() {
        return CoordinatorOverview.of(backend.hospitals(), backend.referrals());
    }

    private String message(String key, Locale locale) {
        return messages.getMessage(key, null, key, locale);
    }
}
