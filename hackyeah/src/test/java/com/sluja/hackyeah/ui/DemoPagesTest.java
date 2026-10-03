package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.mock.MockDemoBackend;
import com.sluja.hackyeah.ui.view.NewReferral;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({MockDemoBackend.class, DemoConfig.class})
class DemoPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoBackend backend;

    @Test
    void rootRedirectsToDoctor() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor"));
    }

    @Test
    void doctorPageShowsOriginHospital() throws Exception {
        mockMvc.perform(get("/doctor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Szpital Powiatowy")))
                .andExpect(content().string(containsString("Koordynator")));
    }

    @Test
    void doctorFormIsPrefilledWithStrokeScenario() throws Exception {
        mockMvc.perform(get("/doctor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trombektomia")))
                .andExpect(content().string(containsString("72 l., objawy od 2 h")));
    }

    @Test
    void submittingReferralRedirectsToItsStatusPage() throws Exception {
        String location = mockMvc.perform(post("/doctor/referrals")
                        .param("targetSpecialty", "NEUROLOGY")
                        .param("requiredProcedures", "CT", "THROMBECTOMY")
                        .param("urgency", "TIME_CRITICAL")
                        .param("patientState", "STABLE")
                        .param("requiresIsolation", "false"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Szpital Uniwersytecki")))
                .andExpect(content().string(containsString("Pracownia hemodynamiki zajęta")))
                .andExpect(content().string(containsString("Fala")));

        mockMvc.perform(get(location.replace("/doctor/referrals/", "/fragments/referral/")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"referral-status\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<html"))));
    }

    @Test
    void invalidReferralRerendersFormWithError() throws Exception {
        mockMvc.perform(post("/doctor/referrals")
                        .param("targetSpecialty", "")
                        .param("urgency", "TIME_CRITICAL")
                        .param("patientState", "STABLE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Wybierz specjalność")));
    }

    @Test
    void unknownReferralIs404() throws Exception {
        mockMvc.perform(get("/doctor/referrals/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hospitalPageShowsFlags() throws Exception {
        mockMvc.perform(get("/hospital/2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Szpital Wojewódzki")))
                .andExpect(content().string(containsString("Pracownia hemodynamiki zajęta")));
    }

    @Test
    void hospitalAcceptsRequestAndLoserSeesAlreadyTaken() throws Exception {
        backend.reset();
        Long referralId = backend.createReferral(new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
                Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null));
        Long universityRequest = backend.inbox(3L).getFirst().requestId();
        Long clinicalRequest = backend.inbox(7L).getFirst().requestId();

        mockMvc.perform(get("/fragments/hospital/3/inbox"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Przyjmuję")))
                .andExpect(content().string(containsString("id=\"hospital-requests\"")));

        mockMvc.perform(post("/hospital/3/requests/" + universityRequest + "/accept"))
                .andExpect(redirectedUrl("/hospital/3"))
                .andExpect(flash().attribute("flash", containsString("Pacjent przyjęty")));

        mockMvc.perform(post("/hospital/7/requests/" + clinicalRequest + "/accept"))
                .andExpect(redirectedUrl("/hospital/7"))
                .andExpect(flash().attribute("flash", containsString("już zrealizowane")))
                .andExpect(flash().attribute("flashType", "warn"));

        mockMvc.perform(get("/hospital/7"))
                .andExpect(content().string(containsString("Już zrealizowane")));
        mockMvc.perform(get("/doctor/referrals/" + referralId))
                .andExpect(content().string(containsString("Miejsce potwierdzone")))
                .andExpect(content().string(containsString("+48 12 400 03 03")));
    }

    @Test
    void hospitalDeclinesWithReason() throws Exception {
        backend.reset();
        backend.createReferral(new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
                Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null));
        Long requestId = backend.inbox(3L).getFirst().requestId();

        mockMvc.perform(post("/hospital/3/requests/" + requestId + "/decline").param("reason", "NO_BEDS"))
                .andExpect(redirectedUrl("/hospital/3"));

        mockMvc.perform(get("/hospital/3"))
                .andExpect(content().string(containsString("Odmowa")))
                .andExpect(content().string(containsString("Brak łóżek")));
    }

    @Test
    void hospitalTogglesFlag() throws Exception {
        backend.reset();

        mockMvc.perform(post("/hospital/3/flags/TK_DOWN").param("active", "true"))
                .andExpect(redirectedUrl("/hospital/3"));
        mockMvc.perform(get("/hospital/3"))
                .andExpect(content().string(containsString("aktywna do")));

        mockMvc.perform(post("/hospital/3/flags/TK_DOWN").param("active", "false"));
        mockMvc.perform(get("/hospital/3"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("aktywna do"))));
    }

    @Test
    void unknownHospitalIs404() throws Exception {
        mockMvc.perform(get("/hospital/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void coordinatorPageListsHospitals() throws Exception {
        mockMvc.perform(get("/coordinator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Szpital Uniwersytecki")));
    }

    @Test
    void coordinatorPageLoadsMap() throws Exception {
        mockMvc.perform(get("/coordinator"))
                .andExpect(content().string(containsString("id=\"map\"")))
                .andExpect(content().string(containsString("leaflet@1.9.4")))
                .andExpect(content().string(containsString("/js/coordinator-map.js")));
    }

    @Test
    void mapDataHasTranslatedLabelsLevelsAndDeclines() throws Exception {
        backend.reset();
        backend.createReferral(new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
                Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null));
        backend.decline(7L, backend.inbox(7L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);

        mockMvc.perform(get("/coordinator/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].origin").value(true))
                .andExpect(jsonPath("$[1].name").value("Szpital Wojewódzki"))
                .andExpect(jsonPath("$[1].level").value("high"))
                .andExpect(jsonPath("$[1].flags[0]").value("Pracownia hemodynamiki zajęta"))
                .andExpect(jsonPath("$[2].pendingRequests").value(1))
                .andExpect(jsonPath("$[6].declines[0].label").value("Brak łóżek"))
                .andExpect(jsonPath("$[6].declines[0].count").value(1));
    }

    @Test
    void coordinatorFragmentsShowEscalationsActiveReferralsAndDeclines() throws Exception {
        backend.reset();
        Long active = backend.createReferral(new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
                Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null));
        backend.decline(3L, backend.inbox(3L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);
        Long escalated = backend.createReferral(new NewReferral("INFECTIOUS", Set.of(Procedure.THROMBECTOMY),
                Referral.Urgency.PLANNED, Referral.PatientState.STABLE, true, null));

        mockMvc.perform(get("/fragments/coordinator/actions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"coordinator-actions\"")))
                .andExpect(content().string(containsString("/doctor/referrals/" + escalated)))
                .andExpect(content().string(containsString("szukaj poza regionem")))
                .andExpect(content().string(containsString("Szpital Wojewódzki")));

        mockMvc.perform(get("/fragments/coordinator/flow"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"coordinator-flow\"")))
                .andExpect(content().string(containsString("/doctor/referrals/" + active)))
                .andExpect(content().string(containsString("Brak łóżek")))
                .andExpect(content().string(containsString("Brak odpowiedzi")));
    }

    @Test
    void coordinatorHospitalTableIsAFragment() throws Exception {
        mockMvc.perform(get("/fragments/coordinator/hospitals"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"coordinator-hospitals\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<html"))));
    }

    @Test
    void resetRedirectsBackToLocalPathOnly() throws Exception {
        mockMvc.perform(post("/demo/reset").header("Referer", "https://evil.example/coordinator"))
                .andExpect(redirectedUrl("/coordinator"));
    }
}
