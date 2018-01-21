package com.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;

public class App {
    private static MongoClient client;

    public static void main(String[] args ) {
        MongoClientOptions.Builder builder = MongoClientOptions.builder().connectionsPerHost(100);
        client = new MongoClient(new MongoClientURI("mongodb://m101_user:m101_user@localhost:27017/m101", builder));
        MongoDatabase db = client.getDatabase("m101").withReadPreference(ReadPreference.secondary());

        MongoCollection<BsonDocument> collection = db.getCollection("funnynumbers", BsonDocument.class);

        BsonDocument document = collection.find().limit(1).iterator().next();
        System.out.println(document);
    }
}
