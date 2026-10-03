package com.sluja.hackyeah.ui.view;

/** Outcome of a hospital's accept/decline click. */
public enum DecisionResult {
    OK,
    /** Another hospital accepted first (the REST API answers 409 Conflict here). */
    ALREADY_TAKEN,
    /** The request already expired or was answered. */
    NO_LONGER_PENDING,
    NOT_FOUND
}
