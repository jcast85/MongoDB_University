/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package course;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.m101j.spark.HelloWorldSparkFreeMarkerStyle;
import freemarker.template.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spark.Spark.*;

/**
 * This class encapsulates the controllers for the blog web application.  It delegates all interaction with MongoDB
 * to three Data Access Objects (DAOs).
 * <p/>
 * It is also the entry point into the web application.
 */
public class BlogController {
    private final Configuration cfg;
    private final UserDAO userDAO;
    private final SessionDAO sessionDAO;
    private final BlogPostDAO blogPostDAO;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            new BlogController("mongodb://localhost");
        } else {
            new BlogController(args[0]);
        }
    }

    public BlogController(String mongoURIString) throws IOException {
        final MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURIString));
        final MongoDatabase blogDatabase = mongoClient.getDatabase("blog");

        userDAO = new UserDAO(blogDatabase);
        sessionDAO = new SessionDAO(blogDatabase);
        blogPostDAO = new BlogPostDAO(blogDatabase);

        cfg = createFreemarkerConfiguration();
        port(8082);
        initializeRoutes();
    }

    abstract class FreemarkerBasedRoute extends RouteImpl {
        final Template template;

        /**
         * Constructor
         *
         * @param path The route path which is used for matching. (e.g. /hello, users/:name)
         */
        protected FreemarkerBasedRoute(final String path, final String templateName) throws IOException {
            super(path);
            template = cfg.getTemplate(templateName);
        }

        @Override
        public Object handle(Request request, Response response) {
            StringWriter writer = new StringWriter();
            try {
                doHandle(request, response, writer);
            } catch (Exception e) {
                e.printStackTrace();
                response.redirect("/internal_error");
            }
            return writer;
        }

        protected abstract void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException;

    }

    private void initializeRoutes() throws IOException {
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(HelloWorldSparkFreeMarkerStyle.class, "/freemarker/");

        // this is the blog home page
        get("/", (req, res) -> getBlog(configuration, req, res));

        // handle the signup post
        post("/signup", (req, res) -> postSignUp(configuration, req, res));

        // present signup form for blog
        get("/signup", (req, res) -> getSignUp(configuration, req, res));

        get("/welcome", (req, res) -> getWelcome(configuration, req, res));

        // present the login page
        get("/login", (req, res) -> getLogin(configuration, req, res));

        // process output coming from login form. On success redirect folks to the welcome page
        // on failure, just return an error and let them try again.
        post("/login", (req, res) -> postLogin(configuration, req, res));

        // allows the user to logout of the blog
        get("/logout", (req, res) -> getLogout(configuration, req, res));

        // used to process internal errors
        get("/internal_error", (req, res) -> getInternalError(configuration, req, res));

        get("/newpost", (req, res) -> getNewPost(configuration, req, res));

        post("/newpost", (req, res) -> postNewPost(configuration, req, res));

        get("/post/:permalink", (req, res) -> getPermalinkPost(configuration, req, res));

        post("/newcomment", (req, res) -> postNewComment(configuration, req, res));

        get("/tag/:thetag", (req, res) -> getPostByTag(configuration, req, res));
    }

    private Object getPostByTag(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request));
        SimpleHash root = new SimpleHash();

        String tag = StringEscapeUtils.escapeHtml4(request.params(":thetag"));
        List<Document> posts = blogPostDAO.findByTagDateDescending(tag);

        root.put("myposts", posts);
        if (username != null) {
            root.put("username", username);
        }

        processTemplate(writer, configuration, root.toMap(), "blog_template.ftl");

        return writer;
    }

    private Object postNewComment(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        String name = StringEscapeUtils.escapeHtml4(request.queryParams("commentName"));
        String email = StringEscapeUtils.escapeHtml4(request.queryParams("commentEmail"));
        String body = StringEscapeUtils.escapeHtml4(request.queryParams("commentBody"));
        String permalink = request.queryParams("permalink");

        Document post = blogPostDAO.findByPermalink(permalink);
        if (post == null) {
            response.redirect("/post_not_found");
        }
        // check that comment is good
        else if (name.equals("") || body.equals("")) {
            // bounce this back to the user for correction
            SimpleHash root = new SimpleHash();
            SimpleHash comment = new SimpleHash();

            comment.put("name", name);
            comment.put("email", email);
            comment.put("body", body);
            root.put("comments", comment);
            root.put("post", post);
            root.put("errors", "Post must contain your name and an actual comment");

            processTemplate(writer, configuration, root.toMap(), "entry_template.ftl");
        } else {
            blogPostDAO.addPostComment(name, email, body, permalink);
            response.redirect("/post/" + permalink);
        }
        return writer;
    }

    private Object postNewPost(Configuration configuration, Request request, Response response) {
        StringWriter writer = new StringWriter();

        String title = StringEscapeUtils.escapeHtml4(request.queryParams("subject"));
        String post = StringEscapeUtils.escapeHtml4(request.queryParams("body"));
        String tags = StringEscapeUtils.escapeHtml4(request.queryParams("tags"));

        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request));

        if (username == null) {
            response.redirect("/login");    // only logged in users can post to blog
        }
        else if (title.equals("") || post.equals("")) {
            // redisplay page with errors
            Map<String, String> root = new HashMap<>();
            root.put("errors", "post must contain a title and blog entry.");
            root.put("subject", title);
            root.put("username", username);
            root.put("tags", tags);
            root.put("body", post);
            processTemplate(writer, configuration, root, "newpost_template.ftl");
        } else {
            // extract tags
            ArrayList<String> tagsArray = extractTags(tags);

            // substitute some <p> for the paragraph breaks
            post = post.replaceAll("\\r?\\n", "<p>");

            String permalink = blogPostDAO.addPost(title, post, tagsArray, username);

            // now redirect to the blog permalink
            response.redirect("/post/" + permalink);
        }
        return writer;
    }

    private Object getPermalinkPost(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        String permalink = request.params(":permalink");

        System.out.println("/post: get " + permalink);

        Document post = blogPostDAO.findByPermalink(permalink);
        if (post == null) {
            response.redirect("/post_not_found");
        } else {
            // empty comment to hold new comment in form at bottom of blog entry detail page
            SimpleHash newComment = new SimpleHash();
            newComment.put("name", "");
            newComment.put("email", "");
            newComment.put("body", "");

            SimpleHash root = new SimpleHash();

            root.put("post", post);
            root.put("comments", newComment);

            processTemplate(writer, configuration, root.toMap(), "entry_template.ftl");
        }


        return writer;
    }

    private Object getNewPost(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        String username = request.queryParams("username");

        SimpleHash root = new SimpleHash();
        root.put("username", StringEscapeUtils.escapeHtml4(username));

        processTemplate(writer, configuration, root.toMap(), "newpost_template.ftl");

        return writer;
    }

    private Object getInternalError(Configuration configuration, Request req, Response res) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        SimpleHash root = new SimpleHash();

        root.put("error", "System has encountered an error.");
        processTemplate(writer, configuration, root.toMap(), "error_template.ftl");

        return writer;
    }

    private Object getLogout(Configuration configuration, Request request, Response response) {
        StringWriter writer = new StringWriter();

        String sessionID = getSessionCookie(request);

        if (sessionID == null) {
            // no session to end
            response.redirect("/login");
        } else {
            // deletes from session table
            sessionDAO.endSession(sessionID);

            // this should delete the cookie
            Cookie c = getSessionCookieActual(request);
            c.setMaxAge(0);

            response.raw().addCookie(c);

            response.redirect("/login");
        }

        return writer;
    }

    private Object postLogin(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        String username = request.queryParams("username");
        String password = request.queryParams("password");

        System.out.println("Login: User submitted: " + username + "  " + password);

        Document user = userDAO.validateLogin(username, password);

        if (user != null) {

            // valid user, let's log them in
            String sessionID = sessionDAO.startSession(user.get("_id").toString());

            if (sessionID == null) {
                response.redirect("/internal_error");
            } else {
                // set the cookie for the user's browser
                response.raw().addCookie(new Cookie("session", sessionID));

                response.redirect("/welcome");
            }
        } else {
            SimpleHash root = new SimpleHash();


            root.put("username", StringEscapeUtils.escapeHtml4(username));
            root.put("password", "");
            root.put("login_error", "Invalid Login");

            processTemplate(writer, configuration, root.toMap(), "login.ftl");
        }

        return writer;
    }

    private Object getLogin(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        SimpleHash root = new SimpleHash();

        root.put("username", "");
        root.put("login_error", "");

        processTemplate(writer, configuration, root.toMap(), "login.ftl");

        return writer;
    }

    private Object getWelcome(Configuration configuration, Request request, Response response) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        String cookie = getSessionCookie(request);
        String username = sessionDAO.findUserNameBySessionId(cookie);

        if (username == null) {
            System.out.println("welcome() can't identify the user, redirecting to signup");
            response.redirect("/signup");

        } else {
            SimpleHash root = new SimpleHash();
            root.put("username", username);
            processTemplate(writer, configuration, root.toMap(), "welcome.ftl");
        }

        return writer;
    }

    private Object getSignUp(Configuration configuration, Request req, Response res) throws TemplateModelException {
        StringWriter writer = new StringWriter();

        SimpleHash root = new SimpleHash();

        // initialize values for the form.
        root.put("username", "");
        root.put("password", "");
        root.put("email", "");
        root.put("password_error", "");
        root.put("username_error", "");
        root.put("email_error", "");
        root.put("verify_error", "");

        writer = processTemplate(writer, configuration, root.toMap(), "signup.ftl");

        return writer;
    }

    private Object postSignUp(Configuration configuration, Request request, Response response) {
        StringWriter writer = new StringWriter();

        String email = request.queryParams("email");
        String username = request.queryParams("username");
        String password = request.queryParams("password");
        String verify = request.queryParams("verify");

        HashMap<String, String> root = new HashMap<String, String>();
        root.put("username", StringEscapeUtils.escapeHtml4(username));
        root.put("email", StringEscapeUtils.escapeHtml4(email));

        if (validateSignup(username, password, verify, email, root)) {
            // good user
            System.out.println("Signup: Creating user with: " + username + " " + password);
            if (!userDAO.addUser(username, password, email)) {
                // duplicate user
                root.put("username_error", "Username already in use, Please choose another");
                writer = processTemplate(writer, configuration, root, "/signup.ftl");
            } else {
                // good user, let's start a session
                String sessionID = sessionDAO.startSession(username);
                System.out.println("Session ID is" + sessionID);

                response.raw().addCookie(new Cookie("session", sessionID));
                response.redirect("/welcome");
            }
        } else {
            // bad signup
            System.out.println("User Registration did not validate");
            writer = processTemplate(writer, configuration, root, "/signup.ftl");
        }

        return writer;
    }

    private StringWriter processTemplate(StringWriter writer, Configuration configuration, Map root, String templateFileName) {
        Template template;
        try {
            template = configuration.getTemplate(templateFileName);
            template.process(root, writer);
        } catch (Exception e) {
            halt(500);
            e.printStackTrace();
        }
        return writer;
    }

    private Object getBlog(Configuration configuration, Request request, Response response) throws TemplateModelException {
        String sessionCookie = getSessionCookie(request);
        String username = null;
        if(sessionCookie != null) {
            username = sessionDAO.findUserNameBySessionId(sessionCookie);
        }
        List<Document> postListByDateDescending = blogPostDAO.findByDateDescending(50);

        StringWriter writer = new StringWriter();

        // this is where we would normally load up the blog data
        // but this week, we just display a placeholder.
        SimpleHash root = new SimpleHash();
        if(username!=null) {
            root.put("username", StringEscapeUtils.escapeHtml4(username));
            root.put("myposts", postListByDateDescending.stream().map(
                    post -> new HashMap<String, Object>() {{
                        put("permalink", post.getString("permalink"));
                        put("title", post.getString("title"));
                        put("author", post.getString("author"));
                        put("body", post.getString("body"));
                        put("date", post.getDate("date"));
                        put("comments", post.get("comments", List.class));
                        put("tags", post.get("tags", List.class));
                    }}
            ).collect(Collectors.toList()));
        }

        writer = processTemplate(writer, configuration, root.toMap(), "/blog_template.ftl");

        return writer;
    }

    // helper function to get session cookie as string
    private String getSessionCookie(final Request request) {
        if (request.raw().getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.raw().getCookies()) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // helper function to get session cookie as string
    private Cookie getSessionCookieActual(final Request request) {
        if (request.raw().getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.raw().getCookies()) {
            if (cookie.getName().equals("session")) {
                return cookie;
            }
        }
        return null;
    }

    // tags the tags string and put it into an array
    private ArrayList<String> extractTags(String tags) {

        // probably more efficent ways to do this.
        //
        // whitespace = re.compile('\s')

        tags = tags.replaceAll("\\s", "");
        String tagArray[] = tags.split(",");

        // let's clean it up, removing the empty string and removing dups
        ArrayList<String> cleaned = new ArrayList<String>();
        for (String tag : tagArray) {
            if (!tag.equals("") && !cleaned.contains(tag)) {
                cleaned.add(tag);
            }
        }

        return cleaned;
    }

    // validates that the registration form has been filled out right and username conforms
    public boolean validateSignup(String username, String password, String verify, String email,
                                  HashMap<String, String> errors) {
        String USER_RE = "^[a-zA-Z0-9_-]{3,20}$";
        String PASS_RE = "^.{3,20}$";
        String EMAIL_RE = "^[\\S]+@[\\S]+\\.[\\S]+$";

        errors.put("username_error", "");
        errors.put("password_error", "");
        errors.put("verify_error", "");
        errors.put("email_error", "");

        if (!username.matches(USER_RE)) {
            errors.put("username_error", "invalid username. try just letters and numbers");
            return false;
        }

        if (!password.matches(PASS_RE)) {
            errors.put("password_error", "invalid password.");
            return false;
        }


        if (!password.equals(verify)) {
            errors.put("verify_error", "password must match");
            return false;
        }

        if (!email.equals("")) {
            if (!email.matches(EMAIL_RE)) {
                errors.put("email_error", "Invalid Email Address");
                return false;
            }
        }

        return true;
    }

    private Configuration createFreemarkerConfiguration() {
        Configuration retVal = new Configuration();
        retVal.setClassForTemplateLoading(BlogController.class, "/freemarker");
        return retVal;
    }
}
