package com.mongodb.m101j.spark;

import static spark.Spark.get;

public class HelloWorldSparkStyle {
    public static void main(String[] args) {
        get("/", (req, res) -> "Hello World From Spark");
    }

    // digit "localhost:4567" on browser
}
