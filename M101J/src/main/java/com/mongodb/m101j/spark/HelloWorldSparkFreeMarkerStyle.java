package com.mongodb.m101j.spark;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.bson.BsonString;
import org.bson.Document;

import java.io.StringWriter;

import static spark.Spark.*;

public class HelloWorldSparkFreeMarkerStyle {
    private static MongoClient client;

    public static void main(String[] args) {
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(HelloWorldSparkFreeMarkerStyle.class, "/");

        port(14567);

        get("/", (req, res) ->
                getHelloWorldWriter(configuration, "Freemarker")
        );
        // digit "localhost:14567" on browser

        get("/juri", (req, res) ->
                getHelloWorldWriter(configuration, "Juri")
        );
        // digit "localhost:14567/juri" on browser

        get("/echo/:thing", (req, res) ->
                getHelloWorldWriter(configuration, req.params(":thing"))
        );
        // digit "localhost:14567/echo/<something>" on browser
    }

    private static Object getHelloWorldWriter(Configuration configuration, String name) {
        StringWriter writer = new StringWriter();

        MongoClientOptions.Builder builder = MongoClientOptions.builder().connectionsPerHost(100);
        client = new MongoClient(new MongoClientURI("mongodb://m101_user:m101_user@localhost:27017/m101", builder));
        MongoDatabase db = client.getDatabase("m101").withReadPreference(ReadPreference.secondary());

        MongoCollection<Document> collection = db.getCollection("hello");
        collection.drop();

        Document document = new Document();
        document.append("name", new BsonString("MongoDB"));
        collection.insertOne(document);

        try {
            Template helloTemplate = configuration.getTemplate("/hello.ftl");
            helloTemplate.process(collection.find().first(), writer);
        } catch (Exception e) {
            halt(500);
            e.printStackTrace();
        }
        return writer;
    }
}
