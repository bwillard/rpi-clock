package com.github.bwillard.rpi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

final class HttpServer extends NanoHTTPD {
	private final static Logger LOGGER = Logger.getLogger(ClockLogic.class.getName());
	private final List<RequestHandler> handlers = new ArrayList<>();

	public HttpServer(int port) {
		super(port);
	}
	
	@Override
    public Response serve(IHTTPSession session) {
		LOGGER.log(Level.INFO, "Handeling: " + session.getMethod() + " " + session.getUri());
		for (RequestHandler handler : handlers) {
			if (handler.canHandle(session.getMethod(), session.getUri())) {
				try {
					return handler.handle(session);
				} catch (RuntimeException e) {
					e.printStackTrace();
					return newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>" + e.toString() +"</body></html>");
					
				}
			}
		}
		
		return newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "<html><body>Not Found</body></html>");
    }
	
	public void registerHandler(RequestHandler handler) {
		handlers.add(handler);
	}
}
