package com.bwillard.rpi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the clock state.
 */
final class ClockState {

    @JsonProperty("isTriggered")
    private final boolean isTriggered;
    @JsonProperty("isManuallySet")
    private final boolean isManuallySet;

    @JsonCreator
    ClockState(
            @JsonProperty("isTriggered") boolean isTriggered,
            @JsonProperty("isManuallySet") boolean isManuallySet) {
        this.isTriggered = isTriggered;
        this.isManuallySet = isManuallySet;
    }
}
