package com.wenroe.resonant.model.enums;

public enum AwsAccountStatus {
    ACTIVE,      // Account is working and can be scanned
    INVALID,     // Credentials are invalid
    EXPIRED,     // Credentials have expired
    TESTING      // Initial connection test in progress
}
