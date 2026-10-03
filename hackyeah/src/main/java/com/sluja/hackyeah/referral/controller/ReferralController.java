package com.sluja.hackyeah.referral.controller;

import com.sluja.hackyeah.referral.dto.CreateReferralRequest;
import com.sluja.hackyeah.referral.dto.ReferralCreatedResponse;
import com.sluja.hackyeah.referral.dto.ReferralDetailResponse;
import com.sluja.hackyeah.referral.service.ReferralService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {
    private static final int DEFAULT_LIMIT = 5;

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @PostMapping
    public ResponseEntity<ReferralCreatedResponse> create(
            @Valid @RequestBody CreateReferralRequest request,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) @Min(1) @Max(50) int limit) {

        ReferralCreatedResponse response = referralService.create(request, limit);

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{referralId}")
    public ReferralDetailResponse findById(@PathVariable Long referralId) {
        return referralService.findById(referralId);
    }
}
