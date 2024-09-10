package com.jfrog.ide.idea.log;

public enum ScanEventStatus {
    STARTED("started"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    FAILED("failed");

    private final String status;

    // Constructor
    ScanEventStatus(String status) {
        this.status = status;
    }

    // Getter method
    public String getStatus() {
        return status;
    }

    // Optional: Override toString method to return the status string
    @Override
    public String toString() {
        return status;
    }

    // Optional: Static method to get enum by value
    public static ScanEventStatus fromString(String status) {
        for (ScanEventStatus s : ScanEventStatus.values()) {
            if (s.status.equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("No enum constant with status " + status);
    }
}
