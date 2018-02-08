db.posts.aggregate([
    {$unwind: "$comments"},
    {$group : {_id:"$comments.author", cnt : {$sum:1}}},
    {$sort : {cnt:-1}},
    {$limit:1},
    {$project : {_id:1}}
]);