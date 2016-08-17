package com.bwillard.rpi;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.datastore.DatastoreV1;
import com.google.api.services.datastore.DatastoreV1.BeginTransactionRequest;
import com.google.api.services.datastore.DatastoreV1.BeginTransactionResponse;
import com.google.api.services.datastore.DatastoreV1.CommitRequest;
import com.google.api.services.datastore.DatastoreV1.CommitResponse;
import com.google.api.services.datastore.DatastoreV1.Key;
import com.google.api.services.datastore.DatastoreV1.Mutation;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;
import com.google.api.services.datastore.client.DatastoreFactory;
import com.google.api.services.datastore.client.DatastoreOptions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.api.services.datastore.client.DatastoreHelper.getPropertyMap;

public class DatastoreStorage implements AlarmStorage {
	private final static Logger LOGGER = Logger.getLogger(DatastoreStorage.class.getName());
	private final Datastore datastore;
	
	public DatastoreStorage(String keyFile, String projectId) {
		datastore = DatastoreFactory.get().create(new DatastoreOptions.Builder()
				.dataset(projectId)
				.credential(getCredential(keyFile))
				.build());
	}

	@Override
    public List<ClockEvent> getClockEvents() throws IOException {
        List<ClockEvent> events = new ArrayList<>();
        DatastoreV1.Query.Builder q = DatastoreV1.Query.newBuilder();
        q.addKindBuilder().setName(ClockEvent.DATASTORE_TYPE);

        boolean hasMoreData = true;
        while (hasMoreData) {
            DatastoreV1.RunQueryRequest request = DatastoreV1.RunQueryRequest.newBuilder().setQuery(q).build();
            DatastoreV1.RunQueryResponse response;
            try {
                response = datastore.runQuery(request);
            } catch (DatastoreException e) {
                throw new IOException("Problem getting clock events", e);
            }

            for (DatastoreV1.EntityResult result : response.getBatch().getEntityResultList()) {
                try {
                    Map<String, DatastoreV1.Value> props = getPropertyMap(result.getEntity());
                    String id = result.getEntity().getKey().getPathElement(0).getName();
                    int startTimeHours = (int) props.get("startTimeHours").getIntegerValue();
                    int startTimeMinutes = (int) props.get("startTimeMinutes").getIntegerValue();
                    int durationSeconds = (int) props.get("durationSeconds").getIntegerValue();

                    events.add(new ClockEvent(id, startTimeHours, startTimeMinutes, durationSeconds));
                } catch (RuntimeException e) {
                    DatastoreStorage.LOGGER.log(Level.WARNING, "Problem parsing stored event: " + result.getEntity(), e);
                }
            }
            if (response.getBatch().getMoreResults() == DatastoreV1.QueryResultBatch.MoreResultsType.NOT_FINISHED) {
                ByteString endCursor = response.getBatch().getEndCursor();
                q.setStartCursor(endCursor);
            } else {
                hasMoreData = false;
            }
        }

        return events;
    }

    @Override
    public void addClockEvent(ClockEvent clockEvent) throws IOException {
        DatastoreV1.Entity.Builder entityBuilder = DatastoreV1.Entity.newBuilder();
        // Set the entity key.
        entityBuilder.setKey(getClockKey(clockEvent.getId()));
        entityBuilder.addProperty(DatastoreV1.Property.newBuilder()
                .setName("startTimeHours")
                .setValue(DatastoreV1.Value.newBuilder().setIntegerValue(clockEvent.getStartTimeHours())));
        entityBuilder.addProperty(DatastoreV1.Property.newBuilder()
                .setName("startTimeMinutes")
                .setValue(DatastoreV1.Value.newBuilder().setIntegerValue(clockEvent.getStartTimeMinutes())));
        entityBuilder.addProperty(DatastoreV1.Property.newBuilder()
                .setName("durationSeconds")
                .setValue(DatastoreV1.Value.newBuilder().setIntegerValue(clockEvent.getDurationSeconds())));

        try {
            commitMutation(DatastoreV1.Mutation.newBuilder()
                    .addInsert(entityBuilder.build())
                    .build());
        } catch (DatastoreException e) {
            throw new IOException("Problem adding event: " + clockEvent, e);
        }
    }

    @Override
    public void deleteClockEvent(String id) throws IOException {
        try {
            commitMutation(DatastoreV1.Mutation.newBuilder()
                    .addDelete(getClockKey(id))
                    .build());
        } catch (DatastoreException e) {
            throw new IOException("Problem deleting event: " + id, e);
        }
    }

    private Key getClockKey(String id) {
		return Key.newBuilder().addPathElement(Key.PathElement.newBuilder()
        		.setKind(ClockEvent.DATASTORE_TYPE)
        		.setName(id))
        		.build();
	}
	
	private CommitResponse commitMutation(Mutation mutation) throws DatastoreException {
		BeginTransactionRequest.Builder treq = BeginTransactionRequest.newBuilder();
		BeginTransactionResponse tres = datastore.beginTransaction(treq.build());
		ByteString tx = tres.getTransaction();
		CommitRequest.Builder creq = CommitRequest.newBuilder();
		// Set the transaction to commit.
		creq.setTransaction(tx);
	
        creq.setMutation(mutation);
        
        return datastore.commit(creq.build());
	}
	
	private static Credential getCredential(String keyFile) {
		try (InputStream stream = new FileInputStream(keyFile)) {
			return GoogleCredential.fromStream(stream)
					.createScoped(ImmutableList.of("https://www.googleapis.com/auth/datastore", "https://www.googleapis.com/auth/userinfo.email"));
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
