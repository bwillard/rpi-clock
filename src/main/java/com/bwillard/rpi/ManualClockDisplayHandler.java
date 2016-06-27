package com.bwillard.rpi;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;

/**
 * Allows the clock to be set manually.
 */
public class ManualClockDisplayHandler implements RequestHandler {
    private final ClockLogic clockLogic;

    public ManualClockDisplayHandler(ClockLogic clockLogic) {
        this.clockLogic = clockLogic;
    }

    @Override
    public boolean canHandle(NanoHTTPD.Method method, String uri) {
        return method == NanoHTTPD.Method.POST && "/api/v1/clock".equals(uri);
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        // NB: don't close input stream or response can't be sent.
        InputStream is = session.getInputStream();
        try  {
            byte[] bytes = new byte[is.available()];
            int length = is.read(bytes);
            String body = new String(bytes, 0, length);
            boolean value = Boolean.parseBoolean(body);

            clockLogic.setManual(value);
            return NanoHTTPD.newFixedLengthResponse(Boolean.toString(value));
        } catch (IOException e) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, "<html><body>Server Error: " + e.toString() + "</body></html>");
        }
    }
}
