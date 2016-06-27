package com.bwillard.rpi;

import com.google.common.base.Preconditions;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TwilioClient {
	private final static Logger LOGGER = Logger.getLogger(TwilioClient.class.getName());
	
	private final TwilioRestClient client;
	private final String fromNumber;
	private final String toNumber;
	
	public TwilioClient(String sid, String authToken, String fromNumber, String toNumber) {
		Preconditions.checkNotNull(sid, "Twilio sid is required");
		Preconditions.checkNotNull(authToken, "Twilio authToken is required");
		Preconditions.checkNotNull(fromNumber, "Twilio fromNumber is required");
		Preconditions.checkNotNull(toNumber, "Twilio toNumber is required");
		this.client = new TwilioRestClient(sid, authToken);
		this.fromNumber = fromNumber;
		this.toNumber = toNumber;
	}
	
	public void sendAlert(String text) throws IOException {
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("To", toNumber)); 
		params.add(new BasicNameValuePair("From", fromNumber)); 
		params.add(new BasicNameValuePair("Body", text));   
	    
		MessageFactory messageFactory = client.getAccount().getMessageFactory(); 
	    try {
			Message message = messageFactory.create(params);
			LOGGER.info("Sent message: " + message.toJSON());
		} catch (TwilioRestException e) {
			throw new IOException("Problem sending message: "
					+ e.getErrorCode() + ": " + e.getErrorMessage(), e);
		}
	}
}
