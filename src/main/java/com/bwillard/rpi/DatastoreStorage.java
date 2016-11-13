package com.bwillard.rpi;

import com.google.cloud.AuthCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatastoreStorage implements com.bwillard.rpi.AlarmStorage {
	private final static Logger LOGGER = Logger.getLogger(DatastoreStorage.class.getName());

    private final String projectId;
	private final Datastore datastore;
	
	public DatastoreStorage(String keyFile, String projectId) {
        this.projectId = projectId;
		datastore = DatastoreOptions.newBuilder()
				.setProjectId(projectId)
				.setAuthCredentials(getAuthCredential(keyFile))
				.build()
                .getService();
	}

	@Override
    public List<com.bwillard.rpi.ClockEvent> getClockEvents() throws IOException {
        List<com.bwillard.rpi.ClockEvent> events = new ArrayList<>();
        EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder().setKind(com.bwillard.rpi.ClockEvent.DATASTORE_TYPE);

        boolean hasMoreData = true;
        while (hasMoreData) {
            QueryResults<Entity> response = datastore.run(queryBuilder.build());

            if (!response.hasNext()) {
                return events;
            }
            Entity entity = null;
            while (response.hasNext()) {
                try {
                    entity = response.next();
                    String id = entity.getKey().getName();
                    int startTimeHours = (int) entity.getLong("startTimeHours");
                    int startTimeMinutes = (int) entity.getLong("startTimeMinutes");
                    int durationSeconds = (int) entity.getLong("durationSeconds");

                    events.add(new com.bwillard.rpi.ClockEvent(id, startTimeHours, startTimeMinutes, durationSeconds));
                } catch (RuntimeException e) {
                    DatastoreStorage.LOGGER.log(Level.WARNING, "Problem parsing stored event: " + entity, e);
                }
            }
            if (response.getCursorAfter() != null) {
                queryBuilder.setStartCursor(response.getCursorAfter());
            } else {
                hasMoreData = false;
            }
        }

        return events;
    }

    @Override
    public void addClockEvent(com.bwillard.rpi.ClockEvent clockEvent) throws IOException {
        FullEntity fullEntity = FullEntity.newBuilder(getClockKey(clockEvent.getId()))
                .set("startTimeHours", clockEvent.getStartTimeHours())
                .set("startTimeMinutes", clockEvent.getStartTimeMinutes())
                .set("durationSeconds", clockEvent.getDurationSeconds())
                .build();
        datastore.add(fullEntity);
    }

    @Override
    public void deleteClockEvent(String id) throws IOException {
        datastore.delete(getClockKey(id));
    }

    private Key getClockKey(String id) {
		return Key.newBuilder(projectId, com.bwillard.rpi.ClockEvent.DATASTORE_TYPE, id)
        		.build();
	}

    private static AuthCredentials getAuthCredential(String keyFile) {
        try (InputStream stream = new FileInputStream(keyFile)) {
            return AuthCredentials.createForJson(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
