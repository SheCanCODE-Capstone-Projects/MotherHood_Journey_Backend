package com.motherhood.journey.common.exception;

import java.util.UUID;

public class MotherNotFoundException extends RuntimeException {
    public MotherNotFoundException(UUID motherId) {
        super("Mother not found with ID: " + motherId);
    }
}
