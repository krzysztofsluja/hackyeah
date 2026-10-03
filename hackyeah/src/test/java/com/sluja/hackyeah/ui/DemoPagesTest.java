package com.sluja.hackyeah.ui;

import com.sluja.hackyeah.hospital.entity.HospitalFlag;
import com.sluja.hackyeah.hospital.entity.Procedure;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.ui.view.CandidateView;
import com.sluja.hackyeah.ui.view.NewReferral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The pages end to end on the real backend and the demo seed (data.sql). Seed ids: 1 = origin
 * („Dolina”), 2 = Wojewódzki (overloaded, cath lab busy), 3 = „Wisła”, 5 = „Zachód”. A stroke
 * referral (neurology + CT + thrombectomy) is eligible only at 3 and 5, so wave 1 asks exactly those.
 */
@SpringBootTest(properties = {
        "spring.sql.init.mode=always",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.encoding=UTF-8",
        "demo.origin-hospital-id=1",
        "demo.scenario-flags.2=CATH_LAB_BUSY"})
@AutoConfigureMockMvc
class DemoPagesTest {

    private static final NewReferral STROKE = new NewReferral("NEUROLOGY", Set.of(Procedure.CT, Procedure.THROMBECTOMY),
            Referral.Urgency.TIME_CRITICAL, Referral.PatientState.STABLE, false, null);
    private static final NewReferral NOWHERE_TO_GO = new NewReferral("INFECTIOUS_DISEASES", Set.of(Procedure.THROMBECTOMY),
            Referral.Urgency.PLANNED, Referral.PatientState.STABLE, true, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoBackend backend;

    @BeforeEach
    void resetDemo() {
        backend.reset();
    }

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
                .andExpect(content().string(containsString("Szpital Powiatowy „Dolina” (Myślenice)")))
                .andExpect(content().string(containsString("Koordynator")));
    }

    @Test
    void doctorFormIsPrefilledWithStrokeScenarioAndRealSpecialties() throws Exception {
        mockMvc.perform(get("/doctor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trombektomia")))
                .andExpect(content().string(containsString("72 l., objawy od 2 h")))
                .andExpect(content().string(containsString("Choroby zakaźne")));
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
                .andExpect(content().string(containsString("Centrum Neurologii i Kardiologii „Wisła”")))
                .andExpect(content().string(containsString("Pracownia hemodynamiki zajęta")))
                .andExpect(content().string(containsString("Fala")));

        mockMvc.perform(get(location.replace("/doctor/referrals/", "/fragments/referral/")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"referral-status\"")))
                .andExpect(content().string(not(containsString("<html"))));
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
    void hospitalPageShowsScenarioFlag() throws Exception {
        mockMvc.perform(get("/hospital/2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Wojewódzki Szpital Specjalistyczny")))
                .andExpect(content().string(containsString("Pracownia hemodynamiki zajęta")));
    }

    @Test
    void hospitalAcceptsRequestAndLoserSeesAlreadyTaken() throws Exception {
        Long referralId = backend.createReferral(STROKE);
        Long wislaRequest = backend.inbox(3L).getFirst().requestId();
        Long zachodRequest = backend.inbox(5L).getFirst().requestId();

        mockMvc.perform(get("/fragments/hospital/3/inbox"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Przyjmuję")))
                .andExpect(content().string(containsString("id=\"hospital-requests\"")));

        mockMvc.perform(post("/hospital/3/requests/" + wislaRequest + "/accept"))
                .andExpect(redirectedUrl("/hospital/3"))
                .andExpect(flash().attribute("flash", containsString("Pacjent przyjęty")));

        mockMvc.perform(post("/hospital/5/requests/" + zachodRequest + "/accept"))
                .andExpect(redirectedUrl("/hospital/5"))
                .andExpect(flash().attribute("flash", containsString("już zrealizowane")))
                .andExpect(flash().attribute("flashType", "warn"));

        mockMvc.perform(get("/hospital/5"))
                .andExpect(content().string(containsString("Już zrealizowane")));
        mockMvc.perform(get("/doctor/referrals/" + referralId))
                .andExpect(content().string(containsString("Miejsce potwierdzone")))
                .andExpect(content().string(containsString("+48 12 000 00 03")));
    }

    @Test
    void answeringTwiceIsNoLongerPending() throws Exception {
        backend.createReferral(STROKE);
        Long requestId = backend.inbox(3L).getFirst().requestId();
        backend.decline(3L, requestId, ReferralRequest.DeclineReason.NO_BEDS);

        mockMvc.perform(post("/hospital/3/requests/" + requestId + "/accept"))
                .andExpect(redirectedUrl("/hospital/3"))
                .andExpect(flash().attribute("flash", containsString("już zamknięte")));
    }

    @Test
    void requestOfAnotherHospitalIs404() throws Exception {
        backend.createReferral(STROKE);
        Long wislaRequest = backend.inbox(3L).getFirst().requestId();

        mockMvc.perform(post("/hospital/5/requests/" + wislaRequest + "/accept"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hospitalDeclinesWithReason() throws Exception {
        backend.createReferral(STROKE);
        Long requestId = backend.inbox(3L).getFirst().requestId();

        mockMvc.perform(post("/hospital/3/requests/" + requestId + "/decline").param("reason", "NO_BEDS"))
                .andExpect(redirectedUrl("/hospital/3"));

        mockMvc.perform(get("/hospital/3"))
                .andExpect(content().string(containsString("Odmowa")))
                .andExpect(content().string(containsString("Brak łóżek")));
    }

    @Test
    void hospitalTogglesFlag() throws Exception {
        mockMvc.perform(post("/hospital/3/flags/TK_DOWN").param("active", "true"))
                .andExpect(redirectedUrl("/hospital/3"));
        mockMvc.perform(get("/hospital/3"))
                .andExpect(content().string(containsString("aktywna do")));

        mockMvc.perform(post("/hospital/3/flags/TK_DOWN").param("active", "false"));
        mockMvc.perform(get("/hospital/3"))
                .andExpect(content().string(not(containsString("aktywna do"))));
    }

    @Test
    void flagSetByHospitalChangesTheRanking() {
        backend.activateFlag(5L, HospitalFlag.FlagType.CATH_LAB_BUSY);

        Long referralId = backend.createReferral(STROKE);

        assertThat(backend.referral(referralId).orElseThrow().candidates())
                .extracting(CandidateView::hospitalId)
                .containsExactly(3L);
    }

    @Test
    void resetClearsReferralsAndRestoresScenarioFlags() {
        backend.createReferral(STROKE);
        backend.clearFlag(2L, HospitalFlag.FlagType.CATH_LAB_BUSY);

        backend.reset();

        assertThat(backend.referrals()).isEmpty();
        assertThat(backend.inbox(3L)).isEmpty();
        assertThat(backend.hospital(2L).orElseThrow()
                .flag(HospitalFlag.FlagType.CATH_LAB_BUSY)).isNotNull();
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
                .andExpect(content().string(containsString("Centrum Neurologii i Kardiologii „Wisła”")));
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
        backend.createReferral(STROKE);
        backend.decline(5L, backend.inbox(5L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);

        mockMvc.perform(get("/coordinator/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].origin").value(true))
                .andExpect(jsonPath("$[1].name").value("Wojewódzki Szpital Specjalistyczny"))
                .andExpect(jsonPath("$[1].level").value("high"))
                .andExpect(jsonPath("$[1].flags[0]").value("Pracownia hemodynamiki zajęta"))
                .andExpect(jsonPath("$[2].pendingRequests").value(1))
                .andExpect(jsonPath("$[4].declines[0].label").value("Brak łóżek"))
                .andExpect(jsonPath("$[4].declines[0].count").value(1));
    }

    @Test
    void coordinatorFragmentsShowEscalationsActiveReferralsAndDeclines() throws Exception {
        Long active = backend.createReferral(STROKE);
        backend.decline(3L, backend.inbox(3L).getFirst().requestId(), ReferralRequest.DeclineReason.NO_BEDS);
        Long escalated = backend.createReferral(NOWHERE_TO_GO);

        mockMvc.perform(get("/fragments/coordinator/actions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"coordinator-actions\"")))
                .andExpect(content().string(containsString("/doctor/referrals/" + escalated)))
                .andExpect(content().string(containsString("szukaj poza regionem")))
                .andExpect(content().string(containsString("Wojewódzki Szpital Specjalistyczny")));

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
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void resetRedirectsBackToLocalPathOnly() throws Exception {
        mockMvc.perform(post("/demo/reset").header("Referer", "https://evil.example/coordinator"))
                .andExpect(redirectedUrl("/coordinator"));
    }
}
