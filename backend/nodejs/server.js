var http = require("http");
var fs = require("fs");
var path = require("path");
var mime = require("mime");

var chatServer = require("./lib/chatServer.js");

var cache = {};
var port = 3000;

var server = http.createServer(function(request, response){
    var filePath = false;
    if(request.url == "/"){
        filePath = "public/index.html";
    }else{
        filePath = "public/"+request.url;
    }

    var absPath = "../../frontend/http/"+filePath;
    serveStatic(response, cache, absPath);
});

server.listen(port, function(){
    console.log("Server listening on porn "+port);
});

chatServer.listen(server);

function send404(response){
    response.writeHead(404, {"content-type":"text/plain"});
    response.write("Error 404: resource not found.");
    response.end();
}

function sendFile(response, filePath, fileContens){
    response.writeHead(200, {"content-type":mime.lookup(path.basename(filePath))});
    response.end(fileContens);
}

function serveStatic(response, cache, absPath){
    if(cache[absPath]){
        sendFile(response, absPath, cache[absPath]);
    }else{
        fs.exists(absPath, function(exists){
            if(!exists){
                send404(response);
                return;
            }

            fs.readFile(absPath, function(err, data){
                if(err){
                    send404(response);
                    return;
                }

                cache[absPath] = data;
                sendFile(response, absPath, data);
            });
        });
    }
}