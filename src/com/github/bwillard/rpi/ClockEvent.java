package com.github.bwillard.rpi;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Strings;

final class ClockEvent {
	static final String DATASTORE_TYPE = "ClockEvent";
	
	@JsonProperty("startTimeHours")
	private final int startTimeHours;
	@JsonProperty("startTimeMinutes")
	private final int startTimeMinutes;
	@JsonProperty("durationSeconds")
	private final int durationSeconds;
	
	@JsonProperty("id")
	private String id;

	
	@JsonCreator
	ClockEvent(@JsonProperty("id") String id,
			@JsonProperty("startTimeHours") int startTimeHours,
			@JsonProperty("startTimeMinutes") int startTimeMinutes, 
			@JsonProperty("durationSeconds") int durationSeconds) {
		if (startTimeHours < 0 || startTimeHours > 23) {
			throw new IllegalArgumentException("startTimeHours must be between 0-23: " + startTimeHours);
		}
		if (startTimeMinutes < 0 || startTimeMinutes > 59) {
			throw new IllegalArgumentException("startTimeMinutes must be between 0-59: " + startTimeMinutes);
		}
		if (durationSeconds < 0 || durationSeconds > 60 * 60 * 10) {
			throw new IllegalArgumentException("durationSeconds must be between 0-" + 60 * 60 * 10 + ": " + durationSeconds);
		}
		this.startTimeHours = startTimeHours;
		this.startTimeMinutes = startTimeMinutes;
		this.durationSeconds = durationSeconds;
		this.id = Strings.isNullOrEmpty(id) ? UUID.randomUUID().toString() : id;
	}
	
	boolean isTriggered(Instant instant) {
		DateTime todayStartTime = DateTime.now().withTimeAtStartOfDay().plusHours(startTimeHours).plusMinutes(startTimeMinutes);
		boolean triggeredToday =  todayStartTime.isBefore(instant) && todayStartTime.plusSeconds(durationSeconds).isAfter(instant);
		DateTime todayStartYesterday = todayStartTime.minusDays(1);
		boolean triggeredYesterday =  todayStartYesterday.isBefore(instant) && todayStartYesterday.plusSeconds(durationSeconds).isAfter(instant);
		return triggeredToday || triggeredYesterday;
	}
	
	public String getId() {
		return id;
	}
	
	public int getStartTimeHours() {
		return startTimeHours;
	}
	
	public int getStartTimeMinutes() {
		return startTimeMinutes;
	}
	
	public int getDurationSeconds() {
		return durationSeconds;
	}
}
