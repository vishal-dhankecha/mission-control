'use strict';

// const conParams = {
//     host: "localhost",
//     user: "vishal",
//     password: "vishal@123#",
//     database: "drone"
// };
const conParams = {
    host: "localhost",
    user: "test",
    password: "test@1234",
    database: "drone"
};
exports.getStatus = function(req, res) {
    var mysql = require('mysql');
    var con = mysql.createConnection(conParams);
    con.connect(function(err) {
        if (err) throw err;
        con.query("SELECT * FROM ServerStatus", function (err, result, fields) {
            if (err) throw err;
                var resObject = {
                statusCode: 0,
                responseObject: result,
                message:""
            }
          res.json(resObject);
        //   console.log(resObject);
        });
    });
};


exports.postStatus = function(req, res) {
    var body = req.body;
    var resObject = {
        statusCode: 0,
        responseObject: null,
        message:""
    }
    if(body.status && body.ip && body.port)
    {
        // console.log(req.body);
        var mysql = require('mysql');
        var con = mysql.createConnection(conParams);
        try {
            con.connect(function(err) {
                if (err) throw err;
                // console.log("Connected!");
                var sql = "UPDATE ServerStatus SET status=" + body.status + ", ip='" + body.ip + "', port=" + body.port + " where id = 1";
                con.query(sql, function (err, result) {
                if (err) throw err;
                console.log("1 record updated");
                });
            });
        
        } catch(e) {
            console.log(e);
        }
    }
    else {
        console.log(req.body);
        resObject.statusCode = 1001;
    }
    
    res.send(resObject);
};