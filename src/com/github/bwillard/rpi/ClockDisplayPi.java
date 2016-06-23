package com.github.bwillard.rpi;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

class ClockDisplayPi implements ClockDisplay {
	final GpioPinDigitalOutput redLedPin;
	final GpioPinDigitalOutput greenLedPin;
	private boolean currentState = false;
	
	public ClockDisplayPi(GpioController gpio, Pin redPin, Pin greenPin) {
		redLedPin = gpio.provisionDigitalOutputPin(redPin, "RedLED", PinState.HIGH);
		greenLedPin = gpio.provisionDigitalOutputPin(greenPin, "GreenLED", PinState.LOW);
		
		redLedPin.pulse(1000);
		greenLedPin.pulse(1000);
		redLedPin.pulse(1000);
		greenLedPin.pulse(1000);
		
		redLedPin.setShutdownOptions(true, PinState.LOW);
		greenLedPin.setShutdownOptions(true, PinState.LOW);
	}
	
	public void setState(boolean okToGetUp) {
		redLedPin.setState(okToGetUp ? PinState.LOW : PinState.HIGH);
		greenLedPin.setState(okToGetUp ? PinState.HIGH : PinState.LOW);
		currentState = okToGetUp;
	}
	
	public void shutdown() {
		redLedPin.setState(PinState.LOW);
		greenLedPin.setState(PinState.LOW);
	}
	
	public boolean getState() {
		return currentState;
	}
}
