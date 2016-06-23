package com.github.bwillard.rpi;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;

public interface RequestHandler {
	boolean canHandle(Method method, String uri);
	Response handle(IHTTPSession session);
}
