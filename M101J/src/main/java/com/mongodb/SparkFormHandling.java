package com.mongodb;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static spark.Spark.*;

public class SparkFormHandling {
    public static void main(String[] args) {
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(HelloWorldSparkFreeMarkerStyle.class, "/");

        port(14567);

        get("/", (req, res) ->
                getFormWriter(configuration, "Juri", Arrays.asList("apple", "orange", "banana", "peach", "ananas"))
        );

        post("/favorite_fruit", (req, res) ->
                postFormResultWriter(req.queryParams("fruit"))
        );

    }

    private static Object postFormResultWriter(final String fruit) {
        System.out.println(fruit);
        if(fruit==null) {
            return "Why don't you pick one?";
        } else {
            return "Your favorite fruit is " + fruit;
        }
    }

    private static Object getFormWriter(Configuration configuration, String name, List<String> fruits) {
        StringWriter writer = new StringWriter();
        try {
            Template helloTemplate = configuration.getTemplate("/fruitPicker.ftl");
            helloTemplate.process(new HashMap<String, Object>() {{
                put("name", name);
                put("fruits", fruits);
            }}, writer);
        } catch (Exception e) {
            halt(500);
            e.printStackTrace();
        }
        return writer;
    }
}
