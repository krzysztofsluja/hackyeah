package com.sluja.hackyeah.referral.controller;

import com.sluja.hackyeah.referral.dto.InboxEntry;
import com.sluja.hackyeah.referral.service.RequestResponseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalInboxController {

    private final RequestResponseService requestResponseService;

    public HospitalInboxController(RequestResponseService requestResponseService) {
        this.requestResponseService = requestResponseService;
    }

    @GetMapping("/{hospitalId}/inbox")
    public List<InboxEntry> inbox(@PathVariable Long hospitalId) {
        return requestResponseService.inbox(hospitalId);
    }
}
