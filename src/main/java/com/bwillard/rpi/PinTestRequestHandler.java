package com.bwillard.rpi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Stopwatch;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

final class PinTestRequestHandler implements RequestHandler  {
	private static final Pattern URL_PATTERN = Pattern.compile("/api/v1/pin/([\\d]+)");
	private final GpioController gpio;
	
	public PinTestRequestHandler(GpioController gpio) {
		this.gpio = gpio;
	}
	
	@Override
	public boolean canHandle(Method method, String uri) {
		return method == Method.GET && Pattern.matches(URL_PATTERN.pattern(), uri);
	}

	@Override
	public Response handle(IHTTPSession session) {
		switch (session.getMethod()) {
			case GET:
				Matcher matcher = URL_PATTERN.matcher(session.getUri());
				if (!matcher.matches()) {
					return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Couldn't match regex</body></html>");
				}
				int pinNumber = Integer.parseInt(matcher.group(1));

				
				return swap(pinNumber);
			default:
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "<html><body>Unknown Method: " + session.getMethod() + "</body></html>");
		}
	}
	
	private Response viaPi4j(int pinNumber) {
		System.out.println("Testing pin: " + pinNumber);
		Pin pin = RaspiPin.getPinByName("GPIO " + pinNumber);
		for (GpioPin provisionedPin : gpio.getProvisionedPins()) {
			if (provisionedPin.getPin() == pin) {
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "Pin is already provisioned as: " + provisionedPin.getName());
			}
		}
		GpioPinDigitalOutput outPin = gpio.provisionDigitalOutputPin(pin, "Test Pin", PinState.LOW);
		for (int i = 0; i < 3; i ++) {
			outPin.setState(true);
			Gpio.delay(500);
			outPin.setState(false);
			Gpio.delay(500);
		}

		gpio.unprovisionPin(outPin);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, "Done");
	}
	
	private Response viawiringPi(int pinNumber) {
		Gpio.pinMode(pinNumber, Gpio.OUTPUT);
		for (int i = 0; i < 3; i++) {
			Gpio.digitalWrite(pinNumber, Gpio.HIGH);
			Gpio.delay(500);
	        Gpio.digitalWrite(pinNumber, Gpio.LOW);
	        Gpio.delay(500);
		}
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, "Done");
	}
	
	private Response swap(int pinNumber) {
		boolean currentState = Gpio.digitalRead(pinNumber) != 0;
		Gpio.digitalWrite(pinNumber, !currentState);
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, "Set " + pinNumber + " to " + !currentState);
	}
}
