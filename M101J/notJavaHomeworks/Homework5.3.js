db.grades.aggregate([
    {$unwind : "$scores"}
    ,{$match : {"scores.type" : {$ne : "quiz"}}}
    ,{$group : {_id: {student_id : "$student_id", class_id : "$class_id"}, avgScoreClassStudent : {$avg : "$scores.score"}}}
    ,{$group : {_id: {class_id : "$_id.class_id"}, avgScoreClass : {$avg : "$avgScoreClassStudent"}}}
    ,{$sort : {avgScoreClass : -1}}
    ,{$limit : 1}
    ,{$project : {_id:0, "bestAvgScoreClassId":"$_id.class_id"}}
]);