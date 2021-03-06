package com.bwillard.rpi;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClockDriver {
	private final static Logger LOGGER = Logger.getLogger(ClockDriver.class.getName());
	
	private Thread t;
	private boolean running = true;
	public ClockDriver(
			final ClockDisplay clockDisplay,
			final ClockLogic clockLogic,
			final Duration pollingFrequency) {
		t = new Thread(() -> {
            while (running) {
                clockDisplay.setState(clockLogic.isTriggered(Instant.now()));
                try {
                    Thread.sleep(pollingFrequency.getMillis());
                } catch (InterruptedException | RuntimeException e) {
                    LOGGER.log(Level.FINE, "Caught exception while sleeping", e);
                    clockDisplay.shutdown();
                    return;
                }
            }
        });
		t.start();
	}
	
	public void shutdown() {
		running = false;
		t.interrupt();
		t = null;
	}
}
