package com.bwillard.rpi;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Alarm storage that stores data from local config file.
 */
final class LocalStorage implements AlarmStorage {
    private final static Logger LOGGER = Logger.getLogger(ActionDriver.class.getName());

    private final String configPath;
    private final ObjectMapper mapper;

    private List<ClockEvent> events;

    public LocalStorage(String configPath) {
        this.configPath = configPath;
        JsonFactory messagePackFactory = new JsonFactory();
        messagePackFactory.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        this.mapper = new ObjectMapper(messagePackFactory);
    }

    @Override
    public List<ClockEvent> getClockEvents() throws IOException {
        loadIfNeeded();
        return ImmutableList.copyOf(events);
    }

    @Override
    public void addClockEvent(ClockEvent clockEvent) throws IOException {
        events.add(clockEvent);
        persist();
    }

    @Override
    public void deleteClockEvent(String id) throws IOException {
        for (ClockEvent event : events) {
            if (event.getId().equals(id)) {
                events.remove(event);
                persist();
                return;
            }
        }
        throw new IOException("Couldn't delete item, id not found: " + id);
    }

    private void loadIfNeeded() throws IOException{
        if (null == events) {
            File file = new File(configPath);
            if (!file.exists()) {
                LOGGER.finer("Persisted events not found at: " + configPath);
                events = new ArrayList<>();
            } else {
                LOGGER.finer("Loading persisted events from: " + configPath);
                try (FileInputStream is = new FileInputStream(configPath)) {
                    events = mapper.readValue(is, new TypeReference<List<ClockEvent>>() {
                    });
                }
            }
        }
    }

    private void persist() throws IOException {
        File file = new File(configPath);
        if (!file.exists()) {
            File directory = file.getParentFile();
            if (!directory.exists()) {
                if(!directory.mkdirs()) {
                    throw new IOException("Couldn't create directory structure: " + directory.getAbsolutePath());
                }
            }
            if (!file.createNewFile()) {
                throw new IOException("Couldn't create file: " + configPath);
            }
        }
        try (FileOutputStream os = new FileOutputStream(configPath, false)) {
            mapper.writeValue(os, events);
        }
    }
}
