db.zips.aggregate([
    {$match : {city : {$in : [/^B/, /^D/, /^O/, /^G/, /^N/, /^M/]}}}
    ,{$group : {_id:null, tot_pop : {$sum:"$pop"}}}
    ,{$project : {_id:0, tot_pop:1}}
]);