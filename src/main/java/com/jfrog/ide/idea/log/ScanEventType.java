package com.jfrog.ide.idea.log;

public enum ScanEventType {
    SOURCE_CODE(1);

    private final int type;

    // Constructor
    ScanEventType(int type) {
        this.type = type;
    }

    // Getter method
    public int getType() {
        return type;
    }

    // Optional: Override toString method to return the type as string
    @Override
    public String toString() {
        return Integer.toString(type);
    }

}
