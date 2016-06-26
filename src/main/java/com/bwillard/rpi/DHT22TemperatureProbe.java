package com.bwillard.rpi;

import com.google.common.base.Stopwatch;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.Gpio;

import java.util.concurrent.TimeUnit;

public class DHT22TemperatureProbe implements TemperatureProbe {
	// DHT22 documentation: https://www.sparkfun.com/datasheets/Sensors/Temperature/DHT22.pdf
	// Initial code cribed from: https://github.com/Link184/Pi4jRaspberry/blob/c2a1bfc83a99288bfd5ff6700b4a367f2ac45896/src/main/java/sensor/DHT22.java
	// or: http://stackoverflow.com/questions/28486159/read-temperature-from-dht11-using-pi4j
	private final GpioController gpio;
	private final Pin dataPin;
	private final int pin = 2;
	
	public DHT22TemperatureProbe(GpioController gpio, Pin dataPin) {
		this.gpio = gpio;
		this.dataPin = dataPin;
	}
	
	@Override public long getTemperatureFahrenheit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTemperatureCelsius() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public void test2() {
		System.out.println("Starting");
		GpioPinDigitalOutput outPin = gpio.provisionDigitalOutputPin(dataPin, "DataPin", PinState.HIGH);
		
		outPin.setState(PinState.LOW);
		Gpio.delay(10);
		
		outPin.setState(PinState.HIGH);
		Gpio.delayMicroseconds(40);
		gpio.unprovisionPin(outPin);
		final GpioPinDigitalInput inPin = gpio.provisionDigitalInputPin(dataPin, "DataPin");
		inPin.addListener(new GpioPinListenerDigital() {
			private int change = 0;
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				System.out.println("Change: " + change + " to " + event.getState());
				change++;
				
				if (change == 4) {
					System.out.println("Done");
					inPin.removeAllListeners();
					gpio.unprovisionPin(inPin);
				}
			}
		});
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		inPin.removeAllListeners();
		gpio.unexport(inPin);
	}
	
	public void test() {
		boolean[] values = new boolean[10000];
		System.out.println("Starting");
		Gpio.pinMode(this.pin, Gpio.OUTPUT);
        Gpio.digitalWrite(this.pin, Gpio.LOW);
        Gpio.delay(10);
        Stopwatch sw = Stopwatch.createStarted();
        Gpio.digitalWrite(this.pin, Gpio.HIGH);
        Gpio.delayMicroseconds(40);
        Gpio.pinMode(this.pin, Gpio.INPUT);
        
        for (int i = 0; i < 10000; i++ ) {
        	values[i] = Gpio.digitalRead(this.pin) != 0;
        	//Gpio.delayMicroseconds(1);
        }
        //System.out.println("");
        long elapsed = sw.elapsed(TimeUnit.MICROSECONDS);
        System.out.println("Done: " + elapsed);
        for (boolean b : values) {
        		System.out.print(b ? "1" : "0" ); 
        }
        System.out.println("");
	}

}
