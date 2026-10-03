package com.sluja.hackyeah.hospital.entity;

public enum Procedure {
    CT("Computed Tomography"),
    MRI("Magnetic Resonance Imaging"),
    PCI("Percutaneous Coronary Intervention"),
    ICU("Intensive Care Unit"),
    VENTILATION("Mechanical Ventilation"),
    THROMBOLYSIS("Thrombolysis"),
    THROMBECTOMY("Thrombectomy");

    private final String displayName;

    Procedure(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
