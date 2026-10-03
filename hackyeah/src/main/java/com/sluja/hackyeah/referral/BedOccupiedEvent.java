package com.sluja.hackyeah.referral;

/**
 * An accepted referral took a bed in this hospital. Published inside the accept transaction;
 * listeners that keep their own view of occupancy (the ADT simulator) react after commit.
 */
public record BedOccupiedEvent(Long hospitalId) {
}
