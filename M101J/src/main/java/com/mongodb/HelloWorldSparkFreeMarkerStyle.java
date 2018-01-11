package com.mongodb;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;

public class HelloWorldSparkFreeMarkerStyle {
    public static void main(String[] args) {
        final Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(HelloWorldSparkFreeMarkerStyle.class, "/");

        port(14567);

        get("/", (req, res) ->
                getHelloWorldWriter(configuration, "Freemarker")
        );

        get("/juri", (req, res) ->
                getHelloWorldWriter(configuration, "Juri")
        );

        get("/echo/:thing", (req, res) ->
                getHelloWorldWriter(configuration, req.params(":thing"))
        );
    }

    private static Object getHelloWorldWriter(Configuration configuration, String name) {
        StringWriter writer = new StringWriter();
        try {
            Template helloTemplate = configuration.getTemplate("/hello.ftl");
            Map<String, Object> helloMap = new HashMap<>();
            helloMap.put("name", name);
            helloTemplate.process(helloMap, writer);
        } catch (Exception e) {
            halt(500);
            e.printStackTrace();
        }
        return writer;
    }
}
