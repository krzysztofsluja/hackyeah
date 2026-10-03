package com.sluja.hackyeah.referral.dto;

/**
 * An on-call line handed to the other side. Only ever returned after an accept (both directions,
 * doctor to doctor) or after an escalation (the fallback ranking) - never alongside the ranking of
 * candidates still being asked, or the doctor would just start phoning down the list and we would
 * be back to the sequential calling the waves exist to replace.
 */
public record HospitalContact(Long hospitalId, String name, String dutyPhone) {}
