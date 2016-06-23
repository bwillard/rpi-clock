package com.github.bwillard.rpi;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class ActionDriver {
	private final static Logger LOGGER = Logger.getLogger(ActionDriver.class.getName());
	private final static Duration MIN_TIME_BETWEEN_ACIONS = Duration.standardMinutes(5);
	
	private final GpioPinDigitalInput inputPin;
	
	private DateTime lastAction;

	public ActionDriver(GpioController gpio, Pin listeningPin, final ButtonAction action) {
		this.inputPin = gpio.provisionDigitalInputPin(listeningPin, "ButtonPin", PinPullResistance.PULL_DOWN);
		
		this.inputPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
            	LOGGER.info("Last message: " + lastAction);
            	if (lastAction == null || lastAction.plus(MIN_TIME_BETWEEN_ACIONS).isBefore(DateTime.now())) {
                	LOGGER.info("Triggering event");
                	lastAction = DateTime.now();
                	action.doAction();
            	} else {
            		LOGGER.info("Skipping acting due to ratelimiting");
            	}

            }
        });
	}
	
	public void close() {
		this.inputPin.removeAllListeners();
	}
}
