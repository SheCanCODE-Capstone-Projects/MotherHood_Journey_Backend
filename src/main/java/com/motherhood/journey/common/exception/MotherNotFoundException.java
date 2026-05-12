package com.motherhood.journey.common.exception;

import org.springframework.http.HttpStatus;

public class MotherNotFoundException extends CustomException {

    public MotherNotFoundException(String motherId) {
        super("Mother not found with ID: " + motherId, HttpStatus.NOT_FOUND);
    }
}