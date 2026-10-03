package com.sluja.hackyeah.referral.controller;

import com.sluja.hackyeah.referral.dto.AcceptanceResponse;
import com.sluja.hackyeah.referral.dto.DeclineRequest;
import com.sluja.hackyeah.referral.dto.RequestView;
import com.sluja.hackyeah.referral.service.RequestResponseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class ReferralRequestController {

    private final RequestResponseService requestResponseService;

    public ReferralRequestController(RequestResponseService requestResponseService) {
        this.requestResponseService = requestResponseService;
    }

    /** Returns 409 when another hospital already took the referral. */
    @PostMapping("/{requestId}/accept")
    public AcceptanceResponse accept(@PathVariable Long requestId) {
        return requestResponseService.accept(requestId);
    }

    @PostMapping("/{requestId}/decline")
    public RequestView decline(@PathVariable Long requestId,
                               @Valid @RequestBody DeclineRequest body) {
        return requestResponseService.decline(requestId, body.reason());
    }
}
