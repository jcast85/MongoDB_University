package course;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

public class BlogPostDAO {
    private final MongoCollection<Document> postsCollection;

    public BlogPostDAO(final MongoDatabase blogDatabase) {
        postsCollection = blogDatabase.getCollection("posts");
    }

    // Return a single post corresponding to a permalink
    public Document findByPermalink(String permalink) {

        // XXX HW 3.2,  Work Here
        Document filters = new Document();
        filters.put("permalink", permalink);
        Document post = postsCollection.find(
                filters
        ).first();

        // fix up if a post has no likes
        if (post != null) {
            List<Document> comments = (List<Document>) post.get("comments");
            for (Document comment : comments) {
                if (!comment.containsKey("num_likes")) {
                    comment.put("num_likes", 0);
                }
            }
        }
        return post;
    }

    // Return a list of posts in descending order. Limit determines
    // how many posts are returned.
    public List<Document> findByDateDescending(int limit) {

        // XXX HW 3.2,  Work Here
        // Return a list of DBObjects, each one a post from the posts collection
        return postsCollection.find()
                .sort(Sorts.descending("date"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public List<Document> findByTagDateDescending(final String tag) {
        return postsCollection.find(eq("tags", tag))
                .sort(descending("date"))
                .limit(10)
                .into(new ArrayList<>());
    }

    public String addPost(String title, String body, List tags, String username) {

        System.out.println("inserting blog entry " + title + " " + body);

        String permalink = title.replaceAll("\\s", "_"); // whitespace becomes _
        permalink = permalink.replaceAll("\\W", ""); // get rid of non alphanumeric
        permalink = permalink.toLowerCase();


        // XXX HW 3.2, Work Here
        // Remember that a valid post has the following keys:
        // author, body, permalink, tags, comments, date, title
        //
        // A few hints:
        // - Don't forget to create an empty list of comments
        // - for the value of the date key, today's datetime is fine.
        // - tags are already in list form that implements suitable interface.
        // - we created the permalink for you above.

        // Build the post object and insert it
        Document post = new Document();
        post.put("permalink", permalink);
        post.put("author", username);
        post.put("body", body);
        post.put("tags", tags);
        post.put("comments", new ArrayList<>());
        post.put("date", new Date());
        post.put("title", title);
        postsCollection.insertOne(post);

        return permalink;
    }

    // Append a comment to a blog post
    public void addPostComment(final String name, final String email, final String body,
                               final String permalink) {

        // XXX HW 3.3, Work Here
        // Hints:
        // - email is optional and may come in NULL. Check for that.
        // - best solution uses an update command to the database and a suitable
        //   operator to append the comment on to any existing list of comments
        Document document = new Document("author", name).append("body", body);
        if (email != null && !email.isEmpty()) {
            document.append("email", email);
        }
        Bson updates = Updates.push("comments", document);
        postsCollection.updateOne(Filters.eq("permalink", permalink), updates);

    }

    public void likePost(final String permalink, final int ordinal) {
        //
        //
        // XXX Final Question 4 - work here
        // You must increment the number of likes on the comment in position `ordinal`
        // on the post identified by `permalink`.
        //
        //

        postsCollection.updateOne(
                Filters.eq("permalink", permalink),
                Updates.inc("comments." + ordinal + ".num_likes", 1)
        );
    }
}
