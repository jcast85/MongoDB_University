db.messages.aggregate([
    {$project : {headers : {From : "$headers.From", To : "$headers.To"}}}
    , {$unwind : "$headers.To"}
    , {$group : {_id:{"id":"$_id", "FromTo" : ["$headers.From", "$headers.To"]}, count : {$sum:1}}}
    , {$sort : {"_id.FromTo":1}}
    , {$group : {count:{$sum:1}, _id:"$_id.FromTo"}}
    , {$sort : {count:-1}}
    , {$limit:1}
    //, {$group : {_id:null, count:{$sum:1}}}
], {
    allowDiskUse:true,
    cursor:{}
});
