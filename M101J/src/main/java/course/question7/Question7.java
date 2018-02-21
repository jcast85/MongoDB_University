package course.question7;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Question7 {
    public static void main(String[] args) {
        String mongoURIString = "mongodb://localhost";
        final MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURIString));
        final MongoDatabase m101 = mongoClient.getDatabase("m101");

        List<Document> imageIdList = m101.getCollection("images").find()
                .projection(Projections.include("_id"))
                .into(new ArrayList<>());
        List<Document> imageIdFromAlbumListOfList = m101.getCollection("albums").find().
                projection(Projections.fields(Projections.exclude("_id"), Projections.include("images")))
                .into(new ArrayList<>());

        Set<Integer> imageIdFromAlbumList = new HashSet<>();
        for(Document document : imageIdFromAlbumListOfList) {
            imageIdFromAlbumList.addAll((List<Integer>) document.get("images"));
        }

        List<Integer> imageIdToRemove = imageIdList.stream().filter(
                imageId -> !imageIdFromAlbumList.contains(imageId.getInteger("_id"))
        ).map(
                imageId -> (Integer) imageId.get("_id")
        ).collect(Collectors.toList());


        for(Integer id : imageIdToRemove) {
            m101.getCollection("images").deleteOne(Filters.eq("_id", id));
        }

        System.out.print(m101.getCollection("images").find(
                Filters.eq("tags", "sunrises")
        ).into(new ArrayList<>()).size());

    }
}
