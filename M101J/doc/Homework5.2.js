db.zips.aggregate([
    {$match : {"state": {$in : ["CA", "NY"]}}}
    ,{$group : {_id : {state:"$state",city:"$city"}, tot_pop : {$sum:"$pop"}}}
    ,{$match : {tot_pop : {$gt:25000}}}
    ,{$group : {_id : null, avg_pop : { $avg : "$tot_pop"}}}
    ,{$project : {_id:0, avg_pop:1}}
]);