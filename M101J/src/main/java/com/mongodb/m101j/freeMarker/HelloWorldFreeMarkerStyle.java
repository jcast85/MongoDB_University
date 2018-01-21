package com.mongodb.m101j.freeMarker;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.port;

public class HelloWorldFreeMarkerStyle {
    public static void main(String[] args) {
        port(4567);
        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(
                HelloWorldFreeMarkerStyle.class, "/");
        try {
            Template helloTemplate = configuration.getTemplate("/hello.ftl");
            StringWriter writer = new StringWriter();
            Map<String, Object> helloMap = new HashMap<>();
            helloMap.put("name", "Freemarker");
            helloTemplate.process(helloMap, writer);

            System.out.println(writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
