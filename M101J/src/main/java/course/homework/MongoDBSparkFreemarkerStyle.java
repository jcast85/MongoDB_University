package course.homework;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.bson.Document;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static spark.Spark.get;
import static spark.Spark.halt;

public class MongoDBSparkFreemarkerStyle {
    public static void main(String[] args)  {
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(MongoDBSparkFreemarkerStyle.class, "/freemarker");

        get("/", (req, res) -> geta(configuration));
    }

    private static Object geta(Configuration configuration) {
        MongoClient         client = new MongoClient(new MongoClientURI("mongodb://m101_user:m101_user@localhost:27017/m101"));

        MongoDatabase database = client.getDatabase("m101");
        final MongoCollection<Document> collection = database.getCollection("funnynumbers");

        StringWriter writer = new StringWriter();
        try {
            Template template = configuration.getTemplate("answer.ftl");

            // Not necessary yet to understand this.  It's just to prove that you
            // are able to run a command on a mongod server
            List<Document> results =
                    collection.aggregate(asList(new Document("$group", new Document("_id", "$value")
                                    .append("count", new Document("$sum", 1))),
                            new Document("$match", new Document("count", new Document("$lte", 2))),
                            new Document("$sort", new Document("_id", 1))))
                            .into(new ArrayList<>());

            int answer = 0;
            for (Document cur : results) {
                answer += (Double) cur.get("_id");
            }

            Map<String, String> answerMap = new HashMap<String, String>();
            answerMap.put("answer", Integer.toString(answer));

            template.process(answerMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
            halt(500);
        }
        return writer;
    }

}

