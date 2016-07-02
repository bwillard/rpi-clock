package com.bwillard.rpi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.joda.time.Instant;

import java.io.IOException;
import java.io.InputStream;

/**
 * Allows the clock to be set manually.
 */
public class ManualClockDisplayHandler implements RequestHandler {
    private final ClockLogic clockLogic;
    private final ObjectMapper mapper;

    public ManualClockDisplayHandler(ClockLogic clockLogic) {
        JsonFactory messagePackFactory = new JsonFactory();
        messagePackFactory.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        this.mapper = new ObjectMapper(messagePackFactory);
        this.clockLogic = clockLogic;
    }

    @Override
    public boolean canHandle(NanoHTTPD.Method method, String uri) {
        return (method == NanoHTTPD.Method.POST  || method == NanoHTTPD.Method.GET) && "/api/v1/clock".equals(uri);
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        // NB: don't close input stream or response can't be sent.
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            InputStream is = session.getInputStream();
            try {
                Boolean value = null;
                if (is.available() > 0) {
                    byte[] bytes = new byte[is.available()];

                    int length = is.read(bytes);
                    String body = new String(bytes, 0, length);
                    value = Boolean.parseBoolean(body);
                }

                clockLogic.setManual(value);
                return NanoHTTPD.newFixedLengthResponse("Set to: "
                        + (value == null ? "null" : Boolean.toString(value)));
            } catch (IOException | RuntimeException e) {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                        "<html><body>Server Error: " + e.toString() + "</body></html>");
            }
        } else if (session.getMethod() == NanoHTTPD.Method.GET) {
            try {
                return NanoHTTPD.newFixedLengthResponse(mapper.writeValueAsString(clockLogic.getState(Instant.now())));
            } catch (JsonProcessingException e) {
                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                        "<html><body>Unknown method: " + e.toString() + "</body></html>");
            }
        } else {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML,
                    "<html><body>Unknown method: " + session.getMethod() + "</body></html>");
        }
    }
}
