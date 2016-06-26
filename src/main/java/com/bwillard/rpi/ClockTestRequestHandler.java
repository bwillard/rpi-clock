package com.bwillard.rpi;

import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

final class ClockTestRequestHandler implements RequestHandler  {
	private static final Pattern URL_PATTERN = Pattern.compile("/api/v1/clock");
	private final ClockDisplay clock;
	
	public ClockTestRequestHandler(ClockDisplay clock) {
		this.clock = clock;
	}
	
	@Override
	public boolean canHandle(Method method, String uri) {
		return method == Method.GET && Pattern.matches(URL_PATTERN.pattern(), uri);
	}

	@Override
	public Response handle(IHTTPSession session) {
		switch (session.getMethod()) {
			case GET:
				clock.setState(!clock.getState());
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "Done");
			default:
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "<html><body>Unknown Method: " + session.getMethod() + "</body></html>");
		}
	}
}
