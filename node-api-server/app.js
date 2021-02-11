'use strict';
const express = require('express');
const http = require('http');
const WebSocket  = require('ws');

const app = express();
const port = process.env.PORT || 8999;
const bodyParser = require('body-parser');

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

app.use(function(req, res, next) {
    res.setHeader('charset', 'utf-8')
    res.setHeader("Access-Control-Allow-Origin"," *");
	res.setHeader("Content-Type"," application/json");
	res.setHeader("Access-Control-Max-Age"," 3600");
    next();
});

var routes = require('./src/routs/routs'); //importing route
routes(app);



// app.listen(port);

// console.log('todo list RESTful API server started on: ' + port);

//initialize a simple http server
const server = http.createServer(app);

//initialize the WebSocket server instance
const wss = new WebSocket.Server({ server });

wss.on('connection', function(ws) {
    ws.id = Math.random();
    //connection is up, let's add a simple simple event
    ws.on('message', function(message) {
        try { 
            if(message && message == "ping"){
                ws.send("ping-success");
            } else {
                //log the received message and send it back to the client
                wss.clients.forEach(function each(client) {
                    if(client.id != ws.id) {
                        client.send(message);
                    }
                });
            }
        } catch(e) {
            console.error(e);
        }
        
    });
});

//start our server
server.listen(port, function(){
    console.log(`Server started on port ${server.address().port} :)`);
});