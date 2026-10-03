package com.sluja.hackyeah.web;

/** Someone got there first - the caller should be told the referral is already settled. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
