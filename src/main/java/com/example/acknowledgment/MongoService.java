
package com.example.acknowledgment;

import com.mongodb.client.*;
import org.bson.Document;

public class MongoService {
	private final MongoClient mongoClient;
	private final MongoCollection<Document> collection;

	public MongoService() {
		mongoClient = MongoClients.create("mongodb://localhost:27017");
		MongoDatabase database = mongoClient.getDatabase("ack_db");
		collection = database.getCollection("acknowledgments");
	}

	public boolean hasAcknowledged(String email) {
		return collection.find(new Document("email", email)).first() != null;
	}

	public void acknowledge(String email) {
		if (!isAcknowledged(email)) {
			Document query = new Document("email", email);
			Document result = collection.find(query).first();
			if (result == null)
				collection.insertOne(new Document("email", email).append("acknowledged", true));
			else {

				Document update = new Document("$set", new Document("acknowledged", true));
				collection.updateOne(query, update);
			}

		}
	}

	public boolean isAcknowledged(String email) {
		Document query = new Document("email", email).append("acknowledged", true);
		return collection.find(query).first() != null;
	}
	
	public void resetEmail(String email) {

		Document query = new Document("email", email);
		Document result = collection.find(query).first();
		if (result == null)
			collection.insertOne(new Document("email", email).append("acknowledged", false));
		else {

			Document update = new Document("$set", new Document("acknowledged", false));
			collection.updateOne(query, update);
		}

	}

}
