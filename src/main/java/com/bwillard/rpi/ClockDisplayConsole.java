package com.bwillard.rpi;

import java.util.logging.Logger;

class ClockDisplayConsole implements ClockDisplay {
    private final static Logger LOGGER = Logger.getLogger(ActionDriver.class.getName());

	private boolean currentState = false;
	
	@Override
	public void setState(boolean okToGetUp) {
        LOGGER.info("Clock display, ok to get up: " + okToGetUp);
		currentState = okToGetUp;
	}
	
	@Override
	public void shutdown() {
		LOGGER.fine("Shutting down clock diplay");
	}

	@Override
	public boolean getState() {
		return currentState;
	}
}
