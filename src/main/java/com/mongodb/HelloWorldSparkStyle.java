package com.mongodb;

import static spark.Spark.get;

public class HelloWorldSparkStyle {
    public static void main(String[] args) {
        get("/", (req, res) -> "Hello World From Spark");
    }
}
