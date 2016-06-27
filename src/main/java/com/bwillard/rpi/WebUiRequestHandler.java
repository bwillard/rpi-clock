package com.bwillard.rpi;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Serves the web UI for the clock.
 */
public class WebUiRequestHandler implements RequestHandler {
    private final String content;

    public WebUiRequestHandler() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("index.html");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteStreams.copy(inputStream, bytes);
        this.content = bytes.toString();
    }

    @Override
    public boolean canHandle(NanoHTTPD.Method method, String uri) {
        return method == NanoHTTPD.Method.GET
                && (Strings.isNullOrEmpty(uri) || "/".equals(uri) || "index.html".equalsIgnoreCase(uri));
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        return NanoHTTPD.newFixedLengthResponse(content);
    }
}
