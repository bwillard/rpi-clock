package com.bwillard.rpi;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

final class TwiloAction implements ButtonAction {
	private final static Logger LOGGER = Logger.getLogger(ClockDriver.class.getName());
	
	private TwilioClient client;
	private final String message;
	
	public TwiloAction(TwilioClient client, String message) {
		this.client = client;
		this.message = message;
	}
	@Override
	public void doAction() {
		try {
			LOGGER.info("Sending message: " + message);
			client.sendAlert(message);
		} catch (IOException | RuntimeException e) {
			LOGGER.log(Level.WARNING, "Problem sending text", e);
		}
	}

}
