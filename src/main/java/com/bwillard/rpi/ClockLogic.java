package com.bwillard.rpi;

import com.google.common.collect.ImmutableList;
import org.joda.time.Instant;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ClockLogic {
	private final static Logger LOGGER = Logger.getLogger(ClockLogic.class.getName());
	private final Map<String, ClockEvent> events = new HashMap<>();
	private final AlarmStorage storage;

	private Boolean manual;
	
	ClockLogic(AlarmStorage storage) throws IOException {
		this.storage = storage;
		try {
			for (ClockEvent event : storage.getClockEvents()) {
				LOGGER.log(Level.INFO, "loaded event: " + event.getId());
				events.put(event.getId(), event);
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, e.toString());
			throw new IOException(e);
		}
	}
	
	public void addEvent(ClockEvent event) throws IOException {
	    storage.addClockEvent(event);
		events.put(event.getId(), event);
	}
	
	public boolean deleteEvent(String id) throws IOException {
        storage.deleteClockEvent(id);
		return events.remove(id) != null;
	}
	
	public List<ClockEvent> getEvents() {
		return ImmutableList.copyOf(events.values());
	}

	public void setManual(Boolean manual) {
		this.manual = manual;
	}
	
	public boolean isTriggered(Instant instant) {
		if (manual != null) {
			return manual;
		}
		for (ClockEvent event : events.values()) {
			if (event.isTriggered(instant)) {
				return true;
			}
		}
		return false;
	}

	public ClockState getState(Instant instant) {
		return new ClockState(isTriggered(instant), manual != null);
	}
}
