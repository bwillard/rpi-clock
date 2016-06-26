package com.bwillard.rpi;

import java.util.logging.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

final class GpioPortUtils {
	private final static Logger LOGGER = Logger.getLogger(GpioPortUtils.class.getName());
	public static void listen(final int port, GpioController gpio) {
		Pin pin = RaspiPin.getPinByName("GPIO " + port);
		
		GpioPinDigitalInput inputPin = gpio.provisionDigitalInputPin(pin, "Button on port: " + port, PinPullResistance.PULL_DOWN);
		
		inputPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
            	LOGGER.info("Triggering event on port" + port);
            }
        });
		LOGGER.info("Listenting for event on port" + port);
	}
}
