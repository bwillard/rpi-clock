package com.bwillard.rpi;

import java.io.IOException;
import java.util.List;

/**
 * Interface for storing the alarm settings.
 */
public interface AlarmStorage {
    List<ClockEvent> getClockEvents() throws IOException;

    void addClockEvent(ClockEvent clockEvent) throws IOException;

    void deleteClockEvent(String id) throws IOException;
}
