var cursor = db.grades.aggregate([{"$match" : {"type" : "homework"}}, { "$group" : {"_id" : "$student_id", "minimum" : {$min: "$score"}}}]);
cursor.forEach(function (doc){
    db.grades.remove({"student_id": doc._id, "score" : doc.minimum});
});
