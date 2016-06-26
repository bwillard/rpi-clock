package com.bwillard.rpi;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

final class TemperatureProbeRequestHandler implements RequestHandler  {
	private final DHT22TemperatureProbe probe;
	
	public TemperatureProbeRequestHandler(DHT22TemperatureProbe probe) {
		this.probe = probe;
	}
	
	@Override
	public boolean canHandle(Method method, String uri) {
		return method == Method.GET && "/api/v1/temperature".equalsIgnoreCase(uri);
	}

	@Override
	public Response handle(IHTTPSession session) {
		switch (session.getMethod()) {
			case GET:
				return handleGetRequest();
			default:
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "<html><body>Unknown Method: " + session.getMethod() + "</body></html>");
		}
	}
	
	private Response handleGetRequest() {
		probe.test();
		return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_HTML, "Done");
	}
}
