package com.github.bwillard.rpi;

import static com.google.api.services.datastore.client.DatastoreHelper.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.datastore.DatastoreV1.BeginTransactionRequest;
import com.google.api.services.datastore.DatastoreV1.BeginTransactionResponse;
import com.google.api.services.datastore.DatastoreV1.CommitRequest;
import com.google.api.services.datastore.DatastoreV1.CommitResponse;
import com.google.api.services.datastore.DatastoreV1.Entity;
import com.google.api.services.datastore.DatastoreV1.EntityResult;
import com.google.api.services.datastore.DatastoreV1.Key;
import com.google.api.services.datastore.DatastoreV1.Mutation;
import com.google.api.services.datastore.DatastoreV1.Property;
import com.google.api.services.datastore.DatastoreV1.Query;
import com.google.api.services.datastore.DatastoreV1.QueryResultBatch.MoreResultsType;
import com.google.api.services.datastore.DatastoreV1.RunQueryRequest;
import com.google.api.services.datastore.DatastoreV1.RunQueryResponse;
import com.google.api.services.datastore.DatastoreV1.Value;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;
import com.google.api.services.datastore.client.DatastoreFactory;
import com.google.api.services.datastore.client.DatastoreOptions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

public class DatastoreStorage {
	private final static Logger LOGGER = Logger.getLogger(DatastoreStorage.class.getName());
	private final Datastore datastore;
	
	public DatastoreStorage(String keyFile, String projectId) {
		datastore = DatastoreFactory.get().create(new DatastoreOptions.Builder()
				.dataset(projectId)
				.credential(getCredential(keyFile))
				.build());
	}
	
	public List<ClockEvent> getClockEvents() throws DatastoreException {
		List<ClockEvent> events = new ArrayList<ClockEvent>();
		Query.Builder q = Query.newBuilder();
		q.addKindBuilder().setName(ClockEvent.DATASTORE_TYPE);
		
		boolean hasMoreData = true;
		while (hasMoreData) {
			RunQueryRequest request = RunQueryRequest.newBuilder().setQuery(q).build();
			RunQueryResponse response = datastore.runQuery(request);
			
			for (EntityResult result : response.getBatch().getEntityResultList()) {
				try {
					Map<String, Value> props = getPropertyMap(result.getEntity());
					String id = result.getEntity().getKey().getPathElement(0).getName();
					int startTimeHours = (int) props.get("startTimeHours").getIntegerValue();
					int startTimeMinutes = (int) props.get("startTimeMinutes").getIntegerValue();
					int durationSeconds = (int) props.get("durationSeconds").getIntegerValue();
		
				  events.add(new ClockEvent(id, startTimeHours, startTimeMinutes, durationSeconds));
				} catch (RuntimeException e) {
					LOGGER.log(Level.WARNING, "Problem parsing stored event: " + result.getEntity(), e);
				}
			}
			if (response.getBatch().getMoreResults() == MoreResultsType.NOT_FINISHED) {
			  ByteString endCursor = response.getBatch().getEndCursor();
			  q.setStartCursor(endCursor);
			} else {
				hasMoreData = false;
			}
		}
		
		return events;
	}
	
	public void addClockEvent(ClockEvent clockEvent) throws DatastoreException {
		Entity.Builder entityBuilder = Entity.newBuilder();
        // Set the entity key.
        entityBuilder.setKey(getClockKey(clockEvent.getId()));
        entityBuilder.addProperty(Property.newBuilder()
            .setName("startTimeHours")
            .setValue(Value.newBuilder().setIntegerValue(clockEvent.getStartTimeHours())));
        entityBuilder.addProperty(Property.newBuilder()
                .setName("startTimeMinutes")
                .setValue(Value.newBuilder().setIntegerValue(clockEvent.getStartTimeMinutes())));
        entityBuilder.addProperty(Property.newBuilder()
                .setName("durationSeconds")
                .setValue(Value.newBuilder().setIntegerValue(clockEvent.getDurationSeconds())));
        
        commitMutation(Mutation.newBuilder()
        		.addInsert(entityBuilder.build())
        		.build());
	}
	
	public void deleteClockEvent(String id) throws DatastoreException {
        commitMutation(Mutation.newBuilder()
        		.addDelete(getClockKey(id))
        		.build());
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
