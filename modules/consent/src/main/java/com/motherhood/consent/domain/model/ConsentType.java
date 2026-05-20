package com.motherhood.consent.domain.model;

public enum ConsentType {

    // Mother allows her data to be sent to government systems like HMIS
    GOV_DATA_SHARE,

    // Mother allows the system to send her SMS reminders
    SMS_REMINDERS,

    // Mother allows her data to be used for health research
    RESEARCH,

    // Mother allows her records to be transferred to another facility
    FACILITY_TRANSFER
}