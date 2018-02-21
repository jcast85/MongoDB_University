db.stuff.drop();
for (var i = 0; i < 10000; i++) {
    db.stuff.insertOne({
        "a" : Math.random()*10000,
        "b" : Math.random()*5000,
        "c" : Math.random()*10
    });
}

db.stuff.createIndex({"c" : 1})
db.stuff.createIndex({"a" : 1,"b" : 1})
db.stuff.createIndex({"a" : 1,"c" : 1})
db.stuff.createIndex({"a" : 1,"b" : 1,"c" : -1})

db.stuff.find({'a':{'$lt':10000}, 'b':{'$gt': 5000}}, {'a':1, 'c':1}).sort({'c':-1}).explain();

// look for "indexName" in the query explanation