package com.bwillard.rpi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AlarmRequestHandler implements RequestHandler  {
	private static final Pattern DELETE_URL_PATTERN = Pattern.compile("/api/v1/alarms/([^/]+)");
	private final ClockLogic clockLogic;
	private final ObjectMapper mapper;
	
	public AlarmRequestHandler(ClockLogic clockLogic) {
		JsonFactory messagePackFactory = new JsonFactory();
        messagePackFactory.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        this.mapper = new ObjectMapper(messagePackFactory);
		this.clockLogic = clockLogic;
	}
	
	@Override
	public boolean canHandle(Method method, String uri) {
		return ((method == Method.GET || method == Method.POST) && "/api/v1/alarms".equalsIgnoreCase(uri))
			|| ((method == Method.DELETE) && Pattern.matches(DELETE_URL_PATTERN.pattern(), uri));
	}

	@Override
	public Response handle(IHTTPSession session) {
		switch (session.getMethod()) {
			case GET:
				return handleGetRequest();
			case POST:
				return handlePostRequest(session);
			case DELETE:
				return handleDeleteRequest(session);
			default:
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "<html><body>Unknown Method: " + session.getMethod() + "</body></html>");
		}
	}
	
	private Response handleGetRequest() {
		try {
			return NanoHTTPD.newFixedLengthResponse(mapper.writeValueAsString(clockLogic.getEvents()));
		} catch (JsonProcessingException e) {
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Server Error: " + e.toString() + "</body></html>");
		}
	}
	
	private Response handlePostRequest(IHTTPSession session) {
		// NB: don't close input stream or response can't be sent.
		InputStream is = session.getInputStream();
		try  {
			ClockEvent event = mapper.readValue(is, ClockEvent.class);
			clockLogic.addEvent(event);
			return NanoHTTPD.newFixedLengthResponse(mapper.writeValueAsString(event));
		} catch (IOException e) {
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Server Error: " + e.toString() + "</body></html>");
		}
	}
	
	private Response handleDeleteRequest(IHTTPSession session) {
		Matcher matcher = DELETE_URL_PATTERN.matcher(session.getUri());
		if (!matcher.matches()) {
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Couldn't match regex</body></html>");
		}
		String id = matcher.group(1);
		try {
			if (clockLogic.deleteEvent(id)) {
				return NanoHTTPD.newFixedLengthResponse("OK");
			} else {
				return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Event Not Found: " + id + "</body></html>");
			}
		} catch (IOException e) {
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Server Error: " + e.toString() + "</body></html>");
		}
	}
}
