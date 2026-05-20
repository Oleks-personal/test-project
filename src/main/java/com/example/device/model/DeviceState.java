package com.example.device.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DeviceState {
    AVAILABLE,
    @JsonProperty("in-use")
    IN_USE, 
    INACTIVE;

    @Override
    public String toString() {
        if (this == IN_USE) {
            return "in-use";
        }
        return this.name().toLowerCase();
    }
}