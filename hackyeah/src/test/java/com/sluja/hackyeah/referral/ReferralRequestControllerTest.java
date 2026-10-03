package com.sluja.hackyeah.referral;

import com.sluja.hackyeah.referral.controller.ReferralRequestController;
import com.sluja.hackyeah.referral.dto.AcceptanceResponse;
import com.sluja.hackyeah.referral.dto.HospitalContact;
import com.sluja.hackyeah.referral.dto.RequestView;
import com.sluja.hackyeah.referral.entity.ReferralRequest;
import com.sluja.hackyeah.referral.service.RequestResponseService;
import com.sluja.hackyeah.web.ApiExceptionHandler;
import com.sluja.hackyeah.web.ConflictException;
import com.sluja.hackyeah.web.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest(ReferralRequestController.class)
@Import(ApiExceptionHandler.class)
class ReferralRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestResponseService requestResponseService;

    private static RequestView request(ReferralRequest.RequestStatus status,
                                       ReferralRequest.DeclineReason reason) {
        return new RequestView(
                7L, 3L, "Wisła", 1, status, reason,
                LocalDateTime.of(2026, 10, 3, 12, 0),
                LocalDateTime.of(2026, 10, 3, 12, 3));
    }

    @Test
    void acceptReturnsTheWinningRequest() throws Exception {
        when(requestResponseService.accept(7L)).thenReturn(new AcceptanceResponse(
                request(ReferralRequest.RequestStatus.ACCEPTED, null),
                new HospitalContact(3L, "Wisła", "+48 12 000 00 03"),
                new HospitalContact(1L, "Dolina", "+48 12 000 00 01")));

        mockMvc.perform(post("/api/requests/7/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.requestId").value(7))
                .andExpect(jsonPath("$.request.status").value("ACCEPTED"))
                // Acceptance is the point at which the phone numbers become useful.
                .andExpect(jsonPath("$.acceptingHospital.dutyPhone").value("+48 12 000 00 03"))
                .andExpect(jsonPath("$.originHospital.dutyPhone").value("+48 12 000 00 01"));
    }

    @Test
    void acceptReturns409WhenSomeoneWasFirst() throws Exception {
        when(requestResponseService.accept(7L))
                .thenThrow(new ConflictException("Referral 12 has already been settled"));

        mockMvc.perform(post("/api/requests/7/accept"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Referral 12 has already been settled"));
    }

    @Test
    void acceptReturns404ForAnUnknownRequest() throws Exception {
        when(requestResponseService.accept(99L))
                .thenThrow(new NotFoundException("No referral request 99"));

        mockMvc.perform(post("/api/requests/99/accept"))
                .andExpect(status().isNotFound());
    }

    @Test
    void declineRecordsTheReason() throws Exception {
        when(requestResponseService.decline(eq(7L), eq(ReferralRequest.DeclineReason.NO_BEDS)))
                .thenReturn(request(ReferralRequest.RequestStatus.DECLINED,
                        ReferralRequest.DeclineReason.NO_BEDS));

        mockMvc.perform(post("/api/requests/7/decline")
                        .contentType("application/json")
                        .content("{\"reason\":\"NO_BEDS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("NO_BEDS"));

        verify(requestResponseService).decline(7L, ReferralRequest.DeclineReason.NO_BEDS);
    }

    @Test
    void declineRequiresAReason() throws Exception {
        mockMvc.perform(post("/api/requests/7/decline")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    void declineRejectsAnUnknownReason() throws Exception {
        mockMvc.perform(post("/api/requests/7/decline")
                        .contentType("application/json")
                        .content("{\"reason\":\"BECAUSE\"}"))
                .andExpect(status().isBadRequest());
    }
}
