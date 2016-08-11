package com.bwillard.rpi;


import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import org.joda.time.Duration;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RPiMain {
	private final static Logger LOGGER = Logger.getLogger(RPiMain.class.getName());
    @Option(name="-logFilePath", usage="The file to log to")
    private String logFilePath = "/tmp/binaryClock.log";

	@Option(name="-gcp-project", usage="Google Project Id")
    private String projectId = "pi-project-314";
	
	@Option(name="-gcp-key-file", usage="JSON key file containing GCP creds")
    private String keyFile = "key.json";
	
	@Option(name="-redPin", usage="The GPIO pin contected to the red LED")
    private int redPinNumber = 0;
	
	@Option(name="-greenPin", usage="The GPIO pin contected to the green LED")
    private int greenPinNumber = 1;
	
	@Option(name="-tempDataPinNumber", usage="The GPIO pin to control and read the temperature data")
    private int tempDataPinNumber = 2;
	
	@Option(name="-clockFeq", usage="How frequently (in seconds) the clock display is updated")
    private int clockFreqSec = 15;
	
	@Option(name="-useTwilio", usage="If twilio features should be enabled")
    private boolean useTwilio = true;
	
	@Option(name="-twilioSid", usage="Twilio SID fom https://www.twilio.com/user/account/voice/getting-started")
    private String twilioSid = null;
	
	@Option(name="-twilioAuthToken", usage="Twilio Auth Token fom https://www.twilio.com/user/account/voice/getting-started")
    private String twilioAuthToken = null;
	
	@Option(name="-twilioFromNumber", usage="The number to send Twilio message from, must be configured in Twilio")
    private String twilioFromNumber = null;
	
	@Option(name="-twilioToNumber", usage="The number to send messages to")
    private String twilioToNumber = null;
	
	@Option(name="-twilioButtonPin", usage="The pin that the Twilio button is hooked up")
    private int twilioButtonPin = 4;
	
	@Option(name="-usePiFeatures",
			handler=ExplicitBooleanOptionHandler.class,
			usage="If this is run on a pi and can access the pins, otherwise mock out pin interactions")
    private boolean usePiFeatures = true;
	
	@Option(name="-httpPort", usage="The port to run the http server on")
    private int httpPort = 8080;
	
	private GpioController gpio = null;
	private ClockDriver clockDriver = null;
	private HttpServer httpServer = null;
	// DHT22TemperatureProbe tempProbe = null;
	private ActionDriver twilioActionDriverDriver = null;
	
	public static void main(String[] args) throws Exception {
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		Logger.getGlobal().addHandler(consoleHandler);
		new RPiMain().doMain(args);
	}
	
	private void doMain(String[] args) throws Exception {
		LOGGER.log(Level.INFO, "starting server");
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOGGER.log(Level.SEVERE, "Server Crashed", e);
            e.printStackTrace();
            System.exit(-1);
        });
		CmdLineParser parser = new CmdLineParser(this);
		parser.parseArgument(args);
        Handler fileHandler = new FileHandler(logFilePath);
        fileHandler.setLevel(Level.ALL);
        Logger.getGlobal().addHandler(fileHandler);
		LOGGER.log(Level.INFO, "starting controllers");
		ClockDisplay clockDisplay;
		DatastoreStorage storage = new DatastoreStorage(keyFile, projectId);
		
		TwilioClient twilioClient = null;
		
		if(useTwilio) {
			twilioClient = new TwilioClient(twilioSid, twilioAuthToken, twilioFromNumber, twilioToNumber);
		}

		if (usePiFeatures) {
			gpio = GpioFactory.getInstance();
			
			Pin redPin = RaspiPin.getPinByName("GPIO " + redPinNumber);
			Pin greenPin = RaspiPin.getPinByName("GPIO " + greenPinNumber);
			clockDisplay = new ClockDisplayPi(gpio, redPin, greenPin);
			//Pin tempPin = RaspiPin.getPinByName("GPIO " + tempDataPinNumber);
			// tempProbe = new DHT22TemperatureProbe(gpio, tempPin);
			if (useTwilio) {
				Pin twilioPin = RaspiPin.getPinByName("GPIO " + twilioButtonPin);
				TwiloAction twilioAction = new TwiloAction(twilioClient, 
						"There was a message from the room");
				twilioActionDriverDriver = new ActionDriver(gpio, twilioPin, twilioAction);
			}
		} else {
			clockDisplay = new ClockDisplayConsole();
		}
		ClockLogic clockLogic = new ClockLogic(storage);
		clockDriver = new ClockDriver(clockDisplay, clockLogic, Duration.standardSeconds(clockFreqSec));
		httpServer = new HttpServer(httpPort);
		httpServer.registerHandler(new AlarmRequestHandler(clockLogic));
		httpServer.registerHandler(new ManualClockDisplayHandler(clockLogic));
		if (usePiFeatures) {
			// httpServer.registerHandler(new TemperatureProbeRequestHandler(tempProbe));
			httpServer.registerHandler(new PinTestRequestHandler(gpio));
			httpServer.registerHandler(new ClockTestRequestHandler(clockDisplay));
		}
		httpServer.registerHandler(new WebUiRequestHandler());
		httpServer.start();
		LOGGER.log(Level.INFO, "controllers started");

		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	LOGGER.log(Level.INFO, "Shutting down");
		    	if (clockDriver != null) {
					clockDriver.shutdown();
				}
				if (gpio != null) {
					gpio.shutdown();
				}
				if (httpServer != null) {
					httpServer.stop();
				}
				if (twilioActionDriverDriver != null) {
					twilioActionDriverDriver.close();
				}
		    }
		});
    }
}
