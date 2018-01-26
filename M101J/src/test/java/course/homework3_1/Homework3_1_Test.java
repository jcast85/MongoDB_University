package course.homework3_1;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class Homework3_1_Test {
    MongoClient mongoClient;
    MongoDatabase schoolDatabase;
    MongoCollection<Document> students;

    @Before
    public void init() throws Exception {
        List<String> studentJson = readStudentsJsonFile();
        List<Document> documents = jsonStringListToListOfDocuments(studentJson);
        String mongoURIString = "mongodb://localhost";
        mongoClient = new MongoClient(new MongoClientURI(mongoURIString));
        schoolDatabase = mongoClient.getDatabase("school");
        students = schoolDatabase.getCollection("students");
        students.drop();
        students.insertMany(documents);
    }

    private List<String> readStudentsJsonFile() throws IOException {
        return readFile("src/main/resources/data/students.json");
    }

    @After
    public void close() throws Exception {
        mongoClient = null;
        schoolDatabase = null;
    }

    @Test
    public void test() throws Exception {
        int size = ((List) students.find(Filters.eq("_id", 137)).first().get("scores")).size();
        assertThat(size, is(4));

        MongoCursor<Document> studentIterator = students.find().iterator();

        Map<Object, Bson> mapOfChanges = new HashMap<>();
        while(studentIterator.hasNext()) {
            Document document = studentIterator.next();
            List<Map<String, Object>> scores = document.get("scores", List.class);
            Double minToRemove = scores.stream().filter(
                    score -> score.get("type").equals("homework")
            ).map(
                    score -> (Double) score.get("score")
            ).min(Comparator.comparing(i -> i))
                    .get();
            if(minToRemove!=null) {
                Bson updates = null;
                for(Map<String, Object> score : scores) {
                    if(score.get("score").equals(minToRemove)) {
                        updates = Updates.pull("scores", new Document(score));
                        break;
                    }
                }
                mapOfChanges.put(document.get("_id"), updates);
            }
        }

        for(Object id : mapOfChanges.keySet()) {
            students.updateOne(Filters.eq("_id", id), mapOfChanges.get(id));
        }

        size = ((List) students.find(Filters.eq("_id", 137)).first().get("scores")).size();
        assertThat(size, is(3));
    }

    private List<String> readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        List<String> fileLines = new ArrayList<>();
        try {
            while((line = reader.readLine()) != null) {
                fileLines.add(line);
            }
            return fileLines;
        } finally {
            reader.close();
        }
    }

    private static List<Document> jsonStringListToListOfDocuments(List<String> jsonStringList){
        return jsonStringList.stream().map(
                jsonString -> Document.parse(jsonString)
        ).collect(Collectors.toList());
    }
}
