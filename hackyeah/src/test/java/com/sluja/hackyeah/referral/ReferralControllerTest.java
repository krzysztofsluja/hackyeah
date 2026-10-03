package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.referral.controller.ReferralController;
import com.sluja.hackyeah.referral.dto.CreateReferralRequest;
import com.sluja.hackyeah.referral.dto.DispatchedRequest;
import com.sluja.hackyeah.referral.dto.ExcludedHospital;
import com.sluja.hackyeah.referral.dto.HospitalCandidate;
import com.sluja.hackyeah.referral.dto.ReferralCreatedResponse;
import com.sluja.hackyeah.referral.entity.Referral;
import com.sluja.hackyeah.referral.service.ReferralService;
import com.sluja.hackyeah.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(ReferralController.class)
@Import(ApiExceptionHandler.class)
class ReferralControllerTest {

    private static final String VALID_PAYLOAD = """
            {
              "targetSpecialty": "NEUROLOGY",
              "requiredProcedures": ["CT"],
              "urgency": "TIME_CRITICAL",
              "patientState": "UNSTABLE",
              "requiresIsolation": false,
              "note": "left-sided weakness",
              "originHospitalId": 1
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferralService referralService;

    @BeforeEach
    void stubService() {
        when(referralService.create(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new ReferralCreatedResponse(
                        12L,
                        Referral.ReferralStatus.OPEN,
                        LocalDateTime.of(2026, 10, 3, 12, 0),
                        1,
                        List.of(new DispatchedRequest(7L, 3L, "Near", 1,
                                LocalDateTime.of(2026, 10, 3, 12, 3))),
                        List.of(new HospitalCandidate(1, 3L, "Near", 0.81,
                                Map.of("TRAVEL_TIME", 0.78), 40, 14)),
                        List.of(new ExcludedHospital(5L, "Full", List.of("NO_AVAILABLE_BEDS")))));
    }

    @Test
    void createsReferralAndReturnsRankedCandidates() throws Exception {
        mockMvc.perform(post("/api/referrals")
                        .contentType("application/json")
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/referrals/12"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.wave").value(1))
                .andExpect(jsonPath("$.requests[0].requestId").value(7))
                .andExpect(jsonPath("$.candidates[0].rank").value(1))
                .andExpect(jsonPath("$.candidates[0].hospitalId").value(3))
                .andExpect(jsonPath("$.candidates[0].travelMinutes").value(14))
                .andExpect(jsonPath("$.candidates[0].criterionScores.TRAVEL_TIME").value(0.78))
                .andExpect(jsonPath("$.excluded[0].violations[0]").value("NO_AVAILABLE_BEDS"));
    }

    @Test
    void defaultsLimitToFive() throws Exception {
        mockMvc.perform(post("/api/referrals")
                        .contentType("application/json")
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated());

        verify(referralService).create(any(CreateReferralRequest.class), eq(5));
    }

    @Test
    void honoursLimitQueryParameter() throws Exception {
        mockMvc.perform(post("/api/referrals")
                        .param("limit", "3")
                        .contentType("application/json")
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated());

        verify(referralService).create(any(CreateReferralRequest.class), eq(3));
    }

    @Test
    void rejectsLimitBelowOne() throws Exception {
        mockMvc.perform(post("/api/referrals")
                        .param("limit", "0")
                        .contentType("application/json")
                        .content(VALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.limit").exists());
    }

    @Test
    void rejectsMissingUrgency() throws Exception {
        String payload = """
                {
                  "targetSpecialty": "NEUROLOGY",
                  "patientState": "UNSTABLE",
                  "requiresIsolation": false,
                  "originHospitalId": 1
                }
                """;

        mockMvc.perform(post("/api/referrals")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.urgency").exists());
    }

    /** A null here would otherwise reach the isolation rules and NPE inside the matching engine. */
    @Test
    void rejectsMissingRequiresIsolation() throws Exception {
        String payload = """
                {
                  "targetSpecialty": "NEUROLOGY",
                  "urgency": "PLANNED",
                  "patientState": "STABLE",
                  "originHospitalId": 1
                }
                """;

        mockMvc.perform(post("/api/referrals")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.requiresIsolation").exists());
    }

    @Test
    void rejectsUnknownEnumValue() throws Exception {
        String payload = """
                {
                  "targetSpecialty": "NEUROLOGY",
                  "urgency": "FOO",
                  "patientState": "STABLE",
                  "requiresIsolation": false,
                  "originHospitalId": 1
                }
                """;

        mockMvc.perform(post("/api/referrals")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Malformed request body")));
    }

    @Test
    void rejectsUnknownOriginHospital() throws Exception {
        when(referralService.create(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalArgumentException("Unknown originHospitalId: 999"));

        mockMvc.perform(post("/api/referrals")
                        .contentType("application/json")
                        .content(VALID_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown originHospitalId: 999"));
    }
}
