package com.bwillard.rpi;

import com.google.api.services.datastore.client.DatastoreException;
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
	private final DatastoreStorage storage;

	private Boolean manual;
	
	ClockLogic(DatastoreStorage storage) throws IOException {
		this.storage = storage;
		try {
			for (ClockEvent event : storage.getClockEvents()) {
				LOGGER.log(Level.INFO, "loaded event: " + event.getId());
				events.put(event.getId(), event);
			}
		} catch (DatastoreException e) {
			LOGGER.log(Level.WARNING, e.toString());
			throw new IOException(e);
		}
	}
	
	public void addEvent(ClockEvent event) throws IOException {
		try {
			storage.addClockEvent(event);
		} catch (DatastoreException e) {
			throw new IOException(e);
		}
		
		events.put(event.getId(), event);
	}
	
	public boolean deleteEvent(String id) throws IOException {
		try {
			storage.deleteClockEvent(id);
		} catch (DatastoreException e) {
			throw new IOException(e);
		}
		
		return events.remove(id) != null;
	}
	
	public List<ClockEvent> getEvents() {
		return ImmutableList.copyOf(events.values());
	}

	public void setManual(boolean manual) {
		this.manual = manual;
	}
	
	public boolean isTriggered(Instant instant) {
		boolean computed = false;
		for (ClockEvent event : events.values()) {
			if (event.isTriggered(instant)) {
				computed =  true;
			}
		}

		if (manual != null) {
			if (computed == manual) {
				manual = null;
			} else {
				return manual;
			}
		}
		return computed;
	}

}
