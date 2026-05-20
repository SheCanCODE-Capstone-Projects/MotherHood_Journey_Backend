package com.motherhood.journey.maternal.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that the annotated String is a recognised ICD-10 code
 * from the seeded MoH HMIS subset.
 */
@Documented
@Constraint(validatedBy = Icd10CodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Icd10Code {
    String message() default "ICD-10 code is not recognised in the MoH HMIS subset";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
