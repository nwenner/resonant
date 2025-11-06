package com.wenroe.resonant.model.enums;

public enum ScanStatus {
    PENDING,   // Created but not started
    RUNNING,   // Currently scanning
    SUCCESS,   // Completed successfully
    FAILED     // Failed with error
}
